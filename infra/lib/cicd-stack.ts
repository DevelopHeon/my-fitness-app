import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as iam from "aws-cdk-lib/aws-iam";
import { Construct } from "constructs";

export interface CicdStackProps extends cdk.StackProps {
  repository: ecr.IRepository;
  instance: ec2.IInstance;
  githubOidcSubject: string;
}

export class CicdStack extends cdk.Stack {
  readonly deployRole: iam.Role;

  constructor(scope: Construct, id: string, props: CicdStackProps) {
    super(scope, id, props);

    const provider = new iam.OpenIdConnectProvider(this, "GitHubOidcProvider", {
      url: "https://token.actions.githubusercontent.com",
      clientIds: ["sts.amazonaws.com"],
    });

    this.deployRole = new iam.Role(this, "GitHubDeployRole", {
      roleName: "my-fitness-github-deploy",
      assumedBy: new iam.OpenIdConnectPrincipal(provider, {
        StringEquals: {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub":
            props.githubOidcSubject,
        },
      }),
      description: "GitHub Actions deployment role for My Fitness",
      maxSessionDuration: cdk.Duration.hours(1),
    });

    props.repository.grantPullPush(this.deployRole);
    this.deployRole.addToPolicy(new iam.PolicyStatement({
      actions: ["ecr:DescribeImages"],
      resources: [props.repository.repositoryArn],
    }));

    this.deployRole.addToPolicy(new iam.PolicyStatement({
      actions: ["ssm:GetParameter"],
      resources: [cdk.Stack.of(this).formatArn({
        service: "ssm",
        resource: "parameter",
        resourceName: "my-fitness/prod/instance-id",
      })],
    }));

    const instanceArn = cdk.Stack.of(this).formatArn({
      service: "ec2",
      resource: "instance",
      resourceName: props.instance.instanceId,
    });
    const runShellDocumentArn = cdk.Stack.of(this).formatArn({
      service: "ssm",
      account: "",
      resource: "document",
      resourceName: "AWS-RunShellScript",
    });

    this.deployRole.addToPolicy(new iam.PolicyStatement({
      actions: ["ssm:SendCommand"],
      resources: [instanceArn, runShellDocumentArn],
    }));
    this.deployRole.addToPolicy(new iam.PolicyStatement({
      actions: ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations", "ssm:ListCommands"],
      resources: ["*"],
    }));

    new cdk.CfnOutput(this, "GitHubDeployRoleArn", {
      value: this.deployRole.roleArn,
    });
  }
}
