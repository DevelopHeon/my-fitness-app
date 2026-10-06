#!/usr/bin/env python3
"""Check deployment, rollback, secret preparation and SSM delivery with local stubs."""
import base64
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("deploy-ec2.sh").read_text()
AWS_STUB = '''#!/usr/bin/env python3
import json, os, sys
args = sys.argv[1:]
if args[0] == "ssm":
    name = args[args.index("--name") + 1].rsplit("/", 1)[-1]
    if name == os.environ.get("DENIED_PARAMETER"):
        print("AccessDeniedException", file=sys.stderr)
        sys.exit(1)
    values = {"ecr-repository-uri": "example.invalid/fitness", "db-host": "fake-db", "db-secret-arn": "fake-arn"}
    values.update(json.loads(os.environ["POLICY_PARAMETERS"]))
    if name not in values:
        print("ParameterNotFound", file=sys.stderr)
        sys.exit(1)
    print(values[name])
elif args[0] == "secretsmanager":
    print('{"username":"test-user","password":"test-password"}')
else:
    print("fake-login")
'''
DOCKER_STUB = '''#!/usr/bin/env python3
import json, os, sys
with open(os.environ["DOCKER_CALLS"], "a") as log:
    log.write(json.dumps(sys.argv[1:]) + "\\n")
if sys.argv[1] == "inspect":
    key = sys.argv[3]
    print({"{{.Config.Image}}": "old-image", "{{.HostConfig.Memory}}": "734003200",
           "{{.HostConfig.MemorySwap}}": "1468006400"}.get(key, ""))
if sys.argv[1] == "login":
    sys.stdin.read()
'''

class AppDeploymentTest(unittest.TestCase):
    def deploy(self, parameters, denied="", fail_health=False, action="apply"):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            (directory / "region").write_text("ap-northeast-2")
            environment = directory / "runtime.env"
            environment.write_text("old-environment\n")
            environment.chmod(0o600)
            calls = directory / "docker-calls"
            stubs = {"aws": AWS_STUB, "docker": DOCKER_STUB,
                     "jq": '#!/bin/sh\ncase "$2" in .username) echo test-user;; *) echo test-password;; esac\n',
                     "curl": '#!/usr/bin/env python3\nimport os, pathlib, sys\np = pathlib.Path(os.environ["HEALTH_CALLS"])\nn = int(p.read_text()) if p.exists() else 0\np.write_text(str(n + 1))\nsys.exit(1 if os.environ["FAIL_HEALTH"] == "1" and n < 30 else 0)\n',
                     "sleep": '#!/bin/sh\nexit 0\n'}
            for name, source in stubs.items():
                command = directory / name
                command.write_text(source)
                command.chmod(0o755)
            script = directory / "deploy.sh"
            script.write_text(SCRIPT.replace("/opt/my-fitness/", str(directory) + "/"))
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       POLICY_PARAMETERS=json.dumps(parameters), DENIED_PARAMETER=denied,
                       DOCKER_CALLS=str(calls), HEALTH_CALLS=str(directory / "health-calls"),
                       FAIL_HEALTH="1" if fail_health else "0")
            result = subprocess.run(["bash", str(script), "test-tag", action], env=env,
                                    capture_output=True, text=True, timeout=10)
            self.assertNotIn("test-jev-key", result.stdout + result.stderr)
            self.last_calls = [json.loads(row) for row in calls.read_text().splitlines()] if calls.exists() else []
            return result, environment.read_text(), environment.stat().st_mode & 0o777, calls.exists()

    def test_defaults_and_obsolete_mode_ignored(self):
        result, env, mode, called = self.deploy({"typesafe-api-key": "test-jev-key", "monitoring-password": "test-metrics-password", "ai-policy-mode": "legacy"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("AI_POLICY_MODEL=jev-1.13.0\n", env)
        self.assertIn("AI_POLICY_VERSION=fitness-policy-v1\n", env)
        self.assertIn("TYPESAFE_API_KEY=test-jev-key\n", env)
        self.assertNotIn("AI_POLICY_MODE=", env)
        self.assertNotIn("TYPESAFE_MODEL=", env)
        self.assertEqual(mode, 0o600)
        self.assertTrue(called)

    def test_monitoring_profile_resource_limits_and_rotation(self):
        result, env, _, _ = self.deploy({"typesafe-api-key": "test-jev-key", "monitoring-password": "test-metrics-password"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("SPRING_PROFILES_ACTIVE=prod,monitoring\n", env)
        self.assertIn("APP_MONITORING_PASSWORD=test-metrics-password\n", env)
        self.assertIn("JAVA_TOOL_OPTIONS=-Xms64m -Xmx256m -XX:+UseG1GC\n", env)
        run = next(call for call in self.last_calls if call[0] == "run")
        self.assertEqual(run[run.index("--memory") + 1], "448m")
        self.assertEqual(run[run.index("--memory-swap") + 1], "512m")
        self.assertIn("local", run)
        self.assertNotIn("test-metrics-password", result.stdout + result.stderr)

    def test_preflight_preserves_environment_and_running_container(self):
        result, env, mode, _ = self.deploy({"typesafe-api-key": "test-jev-key", "monitoring-password": "test-metrics-password"}, action="preflight")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(env, "old-environment\n")
        self.assertEqual(mode, 0o600)
        self.assertFalse(any(call[0] in ["rm", "run"] for call in self.last_calls))

    def test_monitoring_password_failure_preserves_running_app(self):
        for value, denied in [(None, ""), ("", ""), (" ", ""), ("test-metrics-password", "monitoring-password")]:
            parameters = {"typesafe-api-key": "test-jev-key"}
            if value is not None:
                parameters["monitoring-password"] = value
            result, env, _, _ = self.deploy(parameters, denied)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(env, "old-environment\n")
            self.assertFalse(any(call[0] in ["rm", "run"] for call in self.last_calls))

    def test_failed_new_app_restores_old_environment_and_memory(self):
        result, env, mode, _ = self.deploy({"typesafe-api-key": "test-jev-key", "monitoring-password": "test-metrics-password"}, fail_health=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(env, "old-environment\n")
        self.assertEqual(mode, 0o600)
        runs = [call for call in self.last_calls if call[0] == "run"]
        self.assertEqual(runs[-1][-1], "old-image")
        self.assertEqual(runs[-1][runs[-1].index("--memory") + 1], "734003200")
        self.assertEqual(runs[-1][runs[-1].index("--memory-swap") + 1], "1468006400")

    def test_explicit_values(self):
        result, env, _, _ = self.deploy({"typesafe-api-key": "test-jev-key", "monitoring-password": "test-metrics-password", "ai-policy-model": "jev-1.13.0", "ai-policy-version": "fitness-policy-v2"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("AI_POLICY_VERSION=fitness-policy-v2\n", env)

    def test_missing_empty_or_inaccessible_key_and_model_stop_before_container(self):
        for parameters, denied in [({}, ""), ({"typesafe-api-key": ""}, ""),
                                   ({"typesafe-api-key": "test-jev-key"}, "typesafe-api-key"),
                                   ({"typesafe-api-key": "test-jev-key"}, "ai-policy-model")]:
            with self.subTest(parameters=bool(parameters), denied=denied):
                result, env, _, called = self.deploy(parameters, denied)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(env, "old-environment\n")
                self.assertFalse(called)

class MonitoringHostSetupTest(unittest.TestCase):
    def test_existing_swap_and_secrets_are_prepared_idempotently(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            (directory / "region").write_text("ap-northeast-2")
            (directory / "meminfo").write_text("MemAvailable: 300000 kB\n")
            (directory / "sysctl.d").mkdir()
            swap = directory / "swapfile"
            with swap.open("wb") as handle:
                handle.truncate(2147483648)
            fstab = directory / "fstab"
            entry = f"{swap} swap swap defaults 0 0\n"
            fstab.write_text(entry)
            stubs = {
                "aws": AWS_STUB,
                "uname": '#!/bin/sh\ncase "$1" in -s) echo Linux;; *) echo aarch64;; esac\n',
                "docker": '#!/bin/sh\nexit 0\n',
                "stat": '#!/bin/sh\necho 2147483648\n',
                "df": '#!/bin/sh\necho "fs 20000000 1000000 19000000 5% /"\n',
                "swapon": f'#!/bin/sh\necho "{swap}"\n',
                "mkswap": '#!/bin/sh\necho "Must not reformat existing swap" >&2\nexit 1\n',
                "chown": '#!/bin/sh\nexit 0\n',
                "sysctl": '#!/bin/sh\nif [ "$1" = -n ]; then echo 60; fi\n',
            }
            for name, source in stubs.items():
                path = directory / name
                path.write_text(source)
                path.chmod(0o755)
            source = Path(__file__).with_name("setup-ec2-monitoring.sh").read_text()
            source = source.replace('"$EUID"', '"0"')
            replacements = {"/opt/my-fitness/": str(directory) + "/", "/swapfile": str(swap),
                            "/etc/fstab": str(fstab), "/proc/meminfo": str(directory / "meminfo"),
                            "/etc/sysctl.d": str(directory / "sysctl.d")}
            for old, new in replacements.items():
                source = source.replace(old, new)
            script = directory / "setup.sh"
            script.write_text(source)
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       POLICY_PARAMETERS=json.dumps({"monitoring-password": "dummy-metrics",
                                                     "grafana-admin-password": "dummy-grafana",
                                                     "slack-webhook-url": "https://hooks.slack.com/services/dummy/test/url"}))
            for _ in range(2):
                result = subprocess.run(["bash", str(script)], env=env, capture_output=True, text=True, timeout=10)
                self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(fstab.read_text(), entry)
            self.assertEqual((directory / "monitoring/previous-swappiness").read_text(), "60\n")
            secrets = directory / "monitoring/secrets"
            self.assertEqual(secrets.stat().st_mode & 0o777, 0o700)
            self.assertEqual((secrets / "app.monitoring.password").stat().st_mode & 0o777, 0o600)
            self.assertEqual((secrets / "app.monitoring.password").read_text(), "dummy-metrics")
            env["DENIED_PARAMETER"] = "slack-webhook-url"
            result = subprocess.run(["bash", str(script)], env=env, capture_output=True, text=True, timeout=10)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual((secrets / "app.monitoring.password").read_text(), "dummy-metrics")


class ReleaseDeploymentTest(unittest.TestCase):
    def test_preflight_failures_do_not_replace_app_or_stop_monitoring(self):
        for failure in ["setup-ec2-monitoring.sh", "deploy-monitoring.sh preflight", "deploy-ec2.sh preflight"]:
            result, calls = self.deploy(failure)
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn("deploy-ec2.sh apply", calls)
            self.assertNotIn("deploy-monitoring.sh stop", calls)

    def test_monitoring_failure_does_not_redeploy_healthy_app(self):
        result, calls = self.deploy("deploy-monitoring.sh apply")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(calls.count("deploy-ec2.sh apply"), 1)
        self.assertEqual(calls, ["setup-ec2-monitoring.sh", "deploy-monitoring.sh preflight",
                               "deploy-ec2.sh preflight", "setup-caddy.sh", "deploy-monitoring.sh stop",
                               "deploy-ec2.sh apply", "deploy-monitoring.sh apply"])

    def deploy(self, failure):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            tag = "a" * 40
            scripts = directory / "monitoring/releases" / tag / "scripts"
            scripts.mkdir(parents=True)
            (directory / "region").write_text("ap-northeast-2")
            calls = directory / "calls"
            for name in ["setup-ec2-monitoring.sh", "deploy-monitoring.sh", "deploy-ec2.sh", "setup-caddy.sh"]:
                source = f'''#!/bin/bash
key="{name}"
if [[ "$key" == deploy-*.sh ]]; then key="$key ${{2:-apply}}"; fi
printf '%s\\n' "$key" >> "$CALLS"
[[ "$key" != "$FAILURE" ]]
'''
                (scripts / name).write_text(source)
            aws = directory / "aws"
            aws.write_text('#!/bin/sh\necho https://example.invalid\n')
            aws.chmod(0o755)
            source = Path(__file__).with_name("deploy-release.sh").read_text()
            script = directory / "release.sh"
            script.write_text(source.replace("/opt/my-fitness/", str(directory) + "/"))
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       CALLS=str(calls), FAILURE=failure)
            result = subprocess.run(["bash", str(script), tag], env=env, capture_output=True, text=True, timeout=10)
            return result, calls.read_text().splitlines()


class MonitoringDeploymentTest(unittest.TestCase):
    def deploy(self, ready_after=0, previous=False):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            release = directory / "release"
            dashboard = release / "monitoring/grafana/dashboards/my-fitness.json"
            dashboard.parent.mkdir(parents=True)
            dashboard.write_text('{}')
            base = directory / "host"
            base.mkdir()
            prior = base / "current"
            if previous:
                config = prior / "monitoring/docker-compose.prod.yml"
                config.parent.mkdir(parents=True)
                config.write_text('services: {}')
            calls = directory / "docker-calls"
            health = directory / "health-calls"
            stubs = {
                "docker": DOCKER_STUB,
                # GNU readlink -f also resolves a missing final path component.
                "readlink": '#!/bin/sh\necho "$2"\n',
                "curl": '#!/usr/bin/env python3\nimport os, pathlib, sys\np = pathlib.Path(os.environ["HEALTH_CALLS"])\nn = int(p.read_text()) if p.exists() else 0\np.write_text(str(n + 1))\nsys.exit(1 if n < int(os.environ["READY_AFTER"]) else 0)\n',
                "sleep": '#!/bin/sh\nexit 0\n',
                "mv": '#!/usr/bin/env python3\nimport os, sys\nos.replace(sys.argv[-2], sys.argv[-1])\n',
            }
            for name, source in stubs.items():
                command = directory / name
                command.write_text(source)
                command.chmod(0o755)
            script = directory / "deploy.sh"
            source = Path(__file__).with_name("deploy-monitoring.sh").read_text()
            script.write_text(source.replace("BASE=/opt/my-fitness/monitoring", "BASE=" + str(base)))
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       DOCKER_CALLS=str(calls), HEALTH_CALLS=str(health), READY_AFTER=str(ready_after))
            result = subprocess.run(["bash", str(script), str(release)], env=env,
                                    capture_output=True, text=True, timeout=15)
            recorded = [json.loads(row) for row in calls.read_text().splitlines()]
            linked = prior.is_symlink() and prior.resolve() == release.resolve()
            return result, recorded, linked, int(health.read_text())

    def test_healthy_deployment_links_release_after_all_four_checks(self):
        result, calls, linked, health_calls = self.deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(linked)
        self.assertEqual(health_calls, 4)
        self.assertFalse(any("stop" in call for call in calls))

    def test_first_failed_deployment_does_not_restore_missing_current(self):
        result, calls, linked, _ = self.deploy(ready_after=1000)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(linked)
        self.assertTrue(any("stop" in call for call in calls))
        self.assertFalse(any("restored" in line for line in result.stderr.splitlines()))
        self.assertFalse(any("/current/monitoring/" in " ".join(call) for call in calls))

    def test_failure_restores_only_existing_previous_configuration(self):
        result, calls, linked, _ = self.deploy(ready_after=1000, previous=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(linked)
        self.assertIn("Previous monitoring configuration restored", result.stderr)
        self.assertEqual(sum("/current/monitoring/" in " ".join(call) for call in calls), 1)


class LocalMonitoringSetupTest(unittest.TestCase):
    def test_secret_creation_permissions_preservation_and_empty_file_rejection(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            scripts = directory / "scripts"
            scripts.mkdir()
            script = scripts / "setup-monitoring.local.sh"
            shutil.copyfile(Path(__file__).with_name(script.name), script)
            for attempt in range(2):
                result = subprocess.run(["bash", str(script)], capture_output=True, text=True, timeout=10)
                self.assertEqual(result.returncode, 0, result.stderr)
                secrets = directory / ".local/monitoring/secrets"
                values = {path.name: path.read_bytes() for path in secrets.iterdir()}
                self.assertEqual(secrets.stat().st_mode & 0o777, 0o700)
                self.assertEqual(len(values), 2)
                for path in secrets.iterdir():
                    self.assertEqual(path.stat().st_mode & 0o777, 0o600)
                    self.assertTrue(values[path.name])
                if attempt == 0:
                    original = values
                else:
                    self.assertEqual(values, original)
            (secrets / "app.monitoring.password").write_text("")
            result = subprocess.run(["bash", str(script)], capture_output=True, text=True, timeout=10)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual((secrets / "grafana_admin_password").read_bytes(), original["grafana_admin_password"])


class SsmDeploymentTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.repository = self.directory / "repository"
        root = Path(__file__).resolve().parent.parent
        shutil.copytree(root / "monitoring", self.repository / "monitoring")
        shutil.copytree(root / "scripts", self.repository / "scripts")
        (self.repository / "private.env").write_text("must-not-be-shipped")
        for args in [["init", "--quiet"], ["add", "."],
                     ["-c", "user.name=Deployment Test", "-c", "user.email=test@example.invalid",
                      "-c", "commit.gpgsign=false", "commit", "--quiet", "-m", "fixture"]]:
            subprocess.run(["git", *args], cwd=self.repository, check=True, capture_output=True)
        self.tag = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=self.repository, text=True).strip()
        self.calls = self.directory / "calls"
        self.parameters = self.directory / "parameters.json"
        commands = self.directory / "bin"
        commands.mkdir()
        aws = commands / "aws"
        aws.write_text('''#!/usr/bin/env python3
import json
import os
import pathlib
import sys
args = sys.argv[1:]
with open(os.environ["SSM_CALLS"], "a") as calls:
    calls.write(json.dumps(args) + "\\n")
status = os.environ["SSM_STATUS"]
if args[1] == "get-command-invocation" and status == "EventuallySuccessful":
    polls = pathlib.Path(os.environ["SSM_POLLS"])
    if args[args.index("--query") + 1] == "Status":
        count = int(polls.read_text()) if polls.exists() else 0
        polls.write_text(str(count + 1))
        status = ["InvocationDoesNotExist", "InProgress", "Success"][min(count, 2)]
    else:
        status = "Success"
if args[1] == "get-parameter":
    print("i-0123456789abcdef0")
elif args[1] == "send-command":
    source = args[args.index("--parameters") + 1].removeprefix("file://")
    pathlib.Path(os.environ["SSM_PARAMETERS"]).write_text(pathlib.Path(source).read_text())
    print("fake-command-id")
elif status in ["AccessDenied", "InvocationDoesNotExist"]:
    print("AccessDeniedException" if status == "AccessDenied" else status, file=sys.stderr)
    sys.exit(1)
elif args[args.index("--query") + 1] == "Status":
    print(status)
else:
    print(json.dumps({"Status": status}))
''')
        aws.chmod(0o755)
        sleep = commands / "sleep"
        sleep.write_text("#!/bin/sh\nexit 0\n")
        sleep.chmod(0o755)
        if shutil.which("sha256sum") is None:
            checksum = commands / "sha256sum"
            checksum.write_text('#!/bin/sh\nexec shasum -a 256 "$@"\n')
            checksum.chmod(0o755)
        self.env = dict(os.environ, PATH=str(commands) + os.pathsep + os.environ["PATH"],
                        SSM_CALLS=str(self.calls), SSM_PARAMETERS=str(self.parameters), SSM_STATUS="Success",
                        SSM_POLLS=str(self.directory / "polls"))

    def deploy(self, tag=None, status="Success"):
        self.env["SSM_STATUS"] = status
        return subprocess.run(["bash", str(self.repository / "scripts/deploy-ssm.sh"), tag or self.tag],
                              env=self.env, capture_output=True, text=True, timeout=60)

    def test_archive_contains_only_committed_production_runtime(self):
        dashboard = self.repository / "monitoring/grafana/dashboards/my-fitness.json"
        committed = dashboard.read_bytes()
        dashboard.write_text("uncommitted-change-must-not-be-shipped")
        result = self.deploy()
        self.assertEqual(result.returncode, 0, result.stderr)
        parameters = json.loads(self.parameters.read_text())
        self.assertEqual(parameters["executionTimeout"], ["900"])
        archive = base64.b64decode(parameters["commands"][2].split()[2])
        self.assertIn(hashlib.sha256(archive).hexdigest(), parameters["commands"][3])
        self.assertIn(self.tag, parameters["commands"][-1])
        with tarfile.open(fileobj=io.BytesIO(archive), mode="r:gz") as release:
            files = {name for name in release.getnames() if release.getmember(name).isfile()}
            self.assertEqual(files, {
                "monitoring/docker-compose.prod.yml", "monitoring/prometheus/prometheus.prod.yml",
                "monitoring/prometheus/alerts.yml", "monitoring/grafana/dashboards/my-fitness.json",
                "monitoring/grafana/provisioning/dashboards/dashboards.yml",
                "monitoring/grafana/provisioning/datasources/prometheus.prod.yml",
                "monitoring/alertmanager/alertmanager.prod.yml", "scripts/deploy-ec2.sh",
                "scripts/setup-caddy.sh", "scripts/setup-ec2-monitoring.sh",
                "scripts/deploy-monitoring.sh", "scripts/deploy-release.sh"})
            self.assertEqual(release.extractfile("monitoring/grafana/dashboards/my-fitness.json").read(), committed)

    def test_invalid_or_unknown_commit_stops_before_aws(self):
        for tag in ["invalid", "0" * 40]:
            with self.subTest(tag=tag):
                result = self.deploy(tag=tag)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(self.calls.exists())

    def test_eventual_invocation_and_in_progress_can_finish_successfully(self):
        result = self.deploy(status="EventuallySuccessful")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.directory / "polls").read_text(), "3")

    def test_oversized_archive_stops_before_aws(self):
        dashboard = self.repository / "monitoring/grafana/dashboards/my-fitness.json"
        dashboard.write_bytes(os.urandom(60000))
        subprocess.run(["git", "-c", "user.name=Deployment Test", "-c", "user.email=test@example.invalid",
                        "-c", "commit.gpgsign=false", "commit", "--quiet", "-am", "큰 archive fixture"],
                       cwd=self.repository, check=True, capture_output=True)
        tag = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=self.repository, text=True).strip()
        result = self.deploy(tag=tag)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("delivery budget", result.stderr)
        self.assertFalse(self.calls.exists())

    def test_failed_timed_out_or_inaccessible_command_is_not_success(self):
        for status in ["Failed", "TimedOut", "AccessDenied", "InProgress"]:
            with self.subTest(status=status):
                result = self.deploy(status=status)
                self.assertNotEqual(result.returncode, 0)
                expected = "AccessDeniedException" if status == "AccessDenied" else "did not succeed"
                self.assertIn(expected, result.stderr)


if __name__ == "__main__":
    unittest.main()
