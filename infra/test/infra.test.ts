import * as cdk from "aws-cdk-lib";
import { Match, Template } from "aws-cdk-lib/assertions";
import { ApplicationStack } from "../lib/application-stack";
import { CicdStack } from "../lib/cicd-stack";
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

test("application uses t4g small and private ECR", () => {
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
    InstanceType: "t4g.small",
    ImageId: {
      "Fn::FindInMap": [Match.anyValue(), { Ref: "AWS::Region" }, "ami"],
    },
  });
  expect(Object.values(template.toJSON().Mappings)).toContainEqual({
    "ap-northeast-2": { ami: "ami-093fb7e528aec34e5" },
  });
  template.hasResourceProperties("AWS::ECR::Repository", {
    RepositoryName: "my-fitness",
    ImageScanningConfiguration: { ScanOnPush: true },
  });
});

test("cicd trust uses immutable GitHub subject on main", () => {
  const app = new cdk.App();
  const network = new NetworkStack(app, "Network");
  const database = new DatabaseStack(app, "Database", {
    vpc: network.vpc,
    securityGroup: network.dbSecurityGroup,
    availabilityZone: network.vpc.publicSubnets[0].availabilityZone,
  });
  const application = new ApplicationStack(app, "Application", {
    vpc: network.vpc,
    securityGroup: network.appSecurityGroup,
    publicSubnet: network.vpc.publicSubnets[0],
    database: database.database,
    databaseSecret: database.secret,
  });
  const stack = new CicdStack(app, "Cicd", {
    repository: application.repository,
    instance: application.instance,
    githubOidcSubject:
      "repo:DevelopHeon@87063007/my-fitness-app@1375438292:ref:refs/heads/main",
  });

  Template.fromStack(stack).hasResourceProperties("AWS::IAM::Role", {
    RoleName: "my-fitness-github-deploy",
    AssumeRolePolicyDocument: {
      Statement: Match.arrayWith([
        Match.objectLike({
          Action: "sts:AssumeRoleWithWebIdentity",
          Condition: {
            StringEquals: {
              "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
              "token.actions.githubusercontent.com:sub":
                "repo:DevelopHeon@87063007/my-fitness-app@1375438292:ref:refs/heads/main",
            },
          },
        }),
      ]),
    },
  });

  Template.fromStack(stack).hasResourceProperties("AWS::IAM::Policy", {
    PolicyDocument: {
      Statement: Match.arrayWith([
        Match.objectLike({
          Action: "ecr:DescribeImages",
          Effect: "Allow",
        }),
      ]),
    },
  });
});
