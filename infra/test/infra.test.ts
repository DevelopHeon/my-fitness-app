import * as cdk from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { ApplicationStack } from "../lib/application-stack";
import { DatabaseStack } from "../lib/database-stack";
import { NetworkStack } from "../lib/network-stack";

test("network has no NAT gateway", () => {
  const app = new cdk.App();
  const stack = new NetworkStack(app, "Network");
  Template.fromStack(stack).resourceCountIs("AWS::EC2::NatGateway", 0);
});

test("database uses a private micro PostgreSQL instance", () => {
  const app = new cdk.App();
  const network = new NetworkStack(app, "Network");
  const stack = new DatabaseStack(app, "Database", {
    vpc: network.vpc,
    securityGroup: network.dbSecurityGroup,
    availabilityZone: network.vpc.publicSubnets[0].availabilityZone,
  });

  Template.fromStack(stack).hasResourceProperties("AWS::RDS::DBInstance", {
    DBInstanceClass: "db.t4g.micro",
    Engine: "postgres",
    MultiAZ: false,
    PubliclyAccessible: false,
    StorageType: "gp3",
  });
});

test("application uses t4g micro and private ECR", () => {
  const app = new cdk.App();
  const network = new NetworkStack(app, "Network");
  const database = new DatabaseStack(app, "Database", {
    vpc: network.vpc,
    securityGroup: network.dbSecurityGroup,
    availabilityZone: network.vpc.publicSubnets[0].availabilityZone,
  });
  const stack = new ApplicationStack(app, "Application", {
    vpc: network.vpc,
    securityGroup: network.appSecurityGroup,
    publicSubnet: network.vpc.publicSubnets[0],
    database: database.database,
    databaseSecret: database.secret,
  });

  const template = Template.fromStack(stack);
  template.hasResourceProperties("AWS::EC2::Instance", {
    InstanceType: "t4g.micro",
  });
  template.hasResourceProperties("AWS::ECR::Repository", {
    RepositoryName: "my-fitness",
    ImageScanningConfiguration: { ScanOnPush: true },
  });
});
