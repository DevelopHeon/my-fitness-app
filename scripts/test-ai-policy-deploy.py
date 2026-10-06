#!/usr/bin/env python3
"""Check deployment parameters, rollback and host preparation with local command stubs."""
import json
import os
from pathlib import Path
import subprocess
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

class PolicyDeployTest(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
