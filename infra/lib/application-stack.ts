import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as iam from "aws-cdk-lib/aws-iam";
import * as rds from "aws-cdk-lib/aws-rds";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";
import * as ssm from "aws-cdk-lib/aws-ssm";
import { Construct } from "constructs";

export interface ApplicationStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
  securityGroup: ec2.ISecurityGroup;
  publicSubnet: ec2.ISubnet;
  database: rds.IDatabaseInstance;
  databaseSecret: secretsmanager.ISecret;
}

export class ApplicationStack extends cdk.Stack {
  readonly repository: ecr.Repository;
  readonly instance: ec2.Instance;
  readonly elasticIp: ec2.CfnEIP;

  constructor(scope: Construct, id: string, props: ApplicationStackProps) {
    super(scope, id, props);

    this.repository = new ecr.Repository(this, "Repository", {
      repositoryName: "my-fitness",
      imageScanOnPush: true,
      imageTagMutability: ecr.TagMutability.IMMUTABLE,
      lifecycleRules: [{
        description: "Keep the most recent release images",
        maxImageCount: 10,
        tagStatus: ecr.TagStatus.ANY,
      }],
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      emptyOnDelete: false,
    });

    const role = new iam.Role(this, "InstanceRole", {
      assumedBy: new iam.ServicePrincipal("ec2.amazonaws.com"),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"),
        iam.ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"),
      ],
    });

    this.repository.grantPull(role);
    props.databaseSecret.grantRead(role);
    role.addToPolicy(new iam.PolicyStatement({
      actions: ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"],
      resources: [cdk.Stack.of(this).formatArn({
        service: "ssm",
        resource: "parameter",
        resourceName: "my-fitness/prod/*",
      })],
    }));

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      "set -euxo pipefail",
      "dnf install -y docker jq",
      "if ! command -v aws >/dev/null 2>&1; then dnf install -y awscli2 || dnf install -y awscli; fi",
      "systemctl enable --now docker",
      "usermod -aG docker ec2-user",
      "mkdir -p /opt/my-fitness",
      "chown ec2-user:ec2-user /opt/my-fitness",
      `printf '%s\\n' '${cdk.Stack.of(this).region}' > /opt/my-fitness/region`,
      "if [ ! -f /swapfile ]; then",
      "  fallocate -l 2G /swapfile",
      "  chmod 600 /swapfile",
      "  mkswap /swapfile",
      "fi",
      "chown root:root /swapfile",
      "chmod 600 /swapfile",
      "if ! swapon --show=NAME --noheadings | grep -Fxq /swapfile; then swapon /swapfile; fi",
      "if ! awk '$1 == \"/swapfile\" {found=1} END {exit !found}' /etc/fstab; then echo '/swapfile swap swap defaults 0 0' >> /etc/fstab; fi",
    );

    this.instance = new ec2.Instance(this, "Instance", {
      vpc: props.vpc,
      vpcSubnets: { subnets: [props.publicSubnet] },
      securityGroup: props.securityGroup,
      instanceType: new ec2.InstanceType("t4g.micro"),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64,
      }),
      role,
      userData,
      requireImdsv2: true,
      detailedMonitoring: false,
      blockDevices: [{
        deviceName: "/dev/xvda",
        volume: ec2.BlockDeviceVolume.ebs(20, {
          encrypted: true,
          volumeType: ec2.EbsDeviceVolumeType.GP3,
          deleteOnTermination: true,
        }),
      }],
    });

    this.elasticIp = new ec2.CfnEIP(this, "ElasticIp", { domain: "vpc" });
    new ec2.CfnEIPAssociation(this, "ElasticIpAssociation", {
      allocationId: this.elasticIp.attrAllocationId,
      instanceId: this.instance.instanceId,
    });

    new ssm.StringParameter(this, "RuntimeInstanceId", {
      parameterName: "/my-fitness/prod/instance-id",
      stringValue: this.instance.instanceId,
    });
    new ssm.StringParameter(this, "RuntimeRepositoryUri", {
      parameterName: "/my-fitness/prod/ecr-repository-uri",
      stringValue: this.repository.repositoryUri,
    });
    new ssm.StringParameter(this, "RuntimeDatabaseHost", {
      parameterName: "/my-fitness/prod/db-host",
      stringValue: props.database.dbInstanceEndpointAddress,
    });
    new ssm.StringParameter(this, "RuntimeDatabaseSecretArn", {
      parameterName: "/my-fitness/prod/db-secret-arn",
      stringValue: props.databaseSecret.secretArn,
    });

    new cdk.CfnOutput(this, "InstanceId", { value: this.instance.instanceId });
    new cdk.CfnOutput(this, "ElasticIpAddress", { value: this.elasticIp.ref });
    new cdk.CfnOutput(this, "RepositoryUri", { value: this.repository.repositoryUri });
    new cdk.CfnOutput(this, "DatabaseEndpoint", {
      value: props.database.dbInstanceEndpointAddress,
    });
  }
}
