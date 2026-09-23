#!/usr/bin/env node
import * as cdk from "aws-cdk-lib";
import { ApplicationStack } from "../lib/application-stack";
import { CicdStack } from "../lib/cicd-stack";
import { DatabaseStack } from "../lib/database-stack";
import { NetworkStack } from "../lib/network-stack";

const app = new cdk.App();
const env: cdk.Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION ?? "ap-northeast-2",
};

const network = new NetworkStack(app, "MyFitnessNetwork", { env });
const primaryPublicSubnet = network.vpc.publicSubnets[0];
if (!primaryPublicSubnet) {
  throw new Error("Public subnet was not created");
}

const database = new DatabaseStack(app, "MyFitnessDatabase", {
  env,
  vpc: network.vpc,
  securityGroup: network.dbSecurityGroup,
  availabilityZone: primaryPublicSubnet.availabilityZone,
});

const application = new ApplicationStack(app, "MyFitnessApplication", {
  env,
  vpc: network.vpc,
  securityGroup: network.appSecurityGroup,
  publicSubnet: primaryPublicSubnet,
  database: database.database,
  databaseSecret: database.secret,
});

new CicdStack(app, "MyFitnessCicd", {
  env,
  repository: application.repository,
  instance: application.instance,
  githubOidcSubject:
    "repo:DevelopHeon@87063007/my-fitness-app@1375438292:ref:refs/heads/main",
});

cdk.Tags.of(app).add("Project", "my-fitness");
cdk.Tags.of(app).add("Environment", "prod");
