#!/usr/bin/env python3
"""Exercise deploy parameter handling with local command stubs; never contact AWS or Docker."""
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
import os, sys
with open(os.environ["DOCKER_CALLS"], "a") as log:
    log.write(sys.argv[1] + "\\n")
if sys.argv[1] == "login":
    sys.stdin.read()
'''

class PolicyDeployTest(unittest.TestCase):
    def deploy(self, parameters, denied=""):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            (directory / "region").write_text("ap-northeast-2")
            environment = directory / "runtime.env"
            environment.write_text("old-environment\n")
            calls = directory / "docker-calls"
            stubs = {"aws": AWS_STUB, "docker": DOCKER_STUB,
                     "jq": '#!/bin/sh\ncase "$2" in .username) echo test-user;; *) echo test-password;; esac\n',
                     "curl": '#!/bin/sh\nexit 0\n'}
            for name, source in stubs.items():
                command = directory / name
                command.write_text(source)
                command.chmod(0o755)
            script = directory / "deploy.sh"
            script.write_text(SCRIPT.replace("/opt/my-fitness/", str(directory) + "/"))
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       POLICY_PARAMETERS=json.dumps(parameters), DENIED_PARAMETER=denied,
                       DOCKER_CALLS=str(calls))
            result = subprocess.run(["bash", str(script), "test-tag"], env=env,
                                    capture_output=True, text=True, timeout=10)
            self.assertNotIn("test-jev-key", result.stdout + result.stderr)
            return result, environment.read_text(), environment.stat().st_mode & 0o777, calls.exists()

    def test_defaults_and_obsolete_mode_ignored(self):
        result, env, mode, called = self.deploy({"typesafe-api-key": "test-jev-key", "ai-policy-mode": "legacy"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("AI_POLICY_MODEL=jev-1.13.0\n", env)
        self.assertIn("AI_POLICY_VERSION=fitness-policy-v1\n", env)
        self.assertIn("TYPESAFE_API_KEY=test-jev-key\n", env)
        self.assertNotIn("AI_POLICY_MODE=", env)
        self.assertNotIn("TYPESAFE_MODEL=", env)
        self.assertEqual(mode, 0o600)
        self.assertTrue(called)

    def test_explicit_values(self):
        result, env, _, _ = self.deploy({"typesafe-api-key": "test-jev-key", "ai-policy-model": "jev-1.13.0", "ai-policy-version": "fitness-policy-v2"})
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

if __name__ == "__main__":
    unittest.main()
