import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as rds from "aws-cdk-lib/aws-rds";
import * as secretsmanager from "aws-cdk-lib/aws-secretsmanager";
import { Construct } from "constructs";

export interface DatabaseStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
  securityGroup: ec2.ISecurityGroup;
  availabilityZone: string;
}

export class DatabaseStack extends cdk.Stack {
  readonly database: rds.DatabaseInstance;
  readonly secret: secretsmanager.ISecret;

  constructor(scope: Construct, id: string, props: DatabaseStackProps) {
    super(scope, id, props);

    this.database = new rds.DatabaseInstance(this, "Postgres", {
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_17_9,
      }),
      instanceType: new ec2.InstanceType("t4g.micro"),
      vpc: props.vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      availabilityZone: props.availabilityZone,
      securityGroups: [props.securityGroup],
      databaseName: "my_fitness",
      credentials: rds.Credentials.fromGeneratedSecret("my_fitness"),
      allocatedStorage: 20,
      maxAllocatedStorage: 50,
      storageType: rds.StorageType.GP3,
      storageEncrypted: true,
      multiAz: false,
      publiclyAccessible: false,
      backupRetention: cdk.Duration.days(7),
      deleteAutomatedBackups: false,
      deletionProtection: true,
      removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
      cloudwatchLogsExports: ["postgresql"],
      autoMinorVersionUpgrade: true,
    });

    if (!this.database.secret) {
      throw new Error("RDS generated secret was not created");
    }
    this.secret = this.database.secret;

    new cdk.CfnOutput(this, "DatabaseEndpoint", {
      value: this.database.dbInstanceEndpointAddress,
    });
    new cdk.CfnOutput(this, "DatabaseSecretArn", {
      value: this.secret.secretArn,
    });
  }
}
