# My Fitness AWS Infrastructure

AWS CDK v2 / TypeScript infrastructure for the production environment.

## Stacks

- `MyFitnessNetwork`: VPC, public/isolated subnets, application/database security groups
- `MyFitnessDatabase`: PostgreSQL 17 `db.t4g.micro`, Single-AZ, encrypted gp3 storage
- `MyFitnessApplication`: `t4g.micro` EC2, ECR, Elastic IP, runtime SSM parameters
- `MyFitnessCicd`: GitHub Actions OIDC provider and least-privilege deploy role

## Region

Production targets `ap-northeast-2` (Seoul).

## Verification

```bash
npm ci
npm run build
npm test -- --runInBand
npx cdk synth
npx cdk diff
```

## Deployment

```bash
AWS_DEFAULT_REGION=ap-northeast-2 npx cdk deploy \
  MyFitnessNetwork \
  MyFitnessDatabase \
  MyFitnessApplication \
  MyFitnessCicd
```

## Runtime deployment

Application releases are not built on EC2.

1. GitHub Actions builds a `linux/arm64` image.
2. The image is pushed to ECR with the Git commit SHA.
3. GitHub assumes the AWS deploy role through OIDC.
4. SSM Run Command invokes `scripts/deploy-ec2.sh`.
5. EC2 pulls the image, starts one Spring Boot container and checks `/actuator/health`.
6. A failed health check restores the previous image.

SSH is not required or opened.

## EC2 bootstrap note

Amazon Linux 2023 includes `curl-minimal`. Do not install the full `curl` package in the bootstrap package set because DNF can stop on a `curl-minimal` package conflict.

The bootstrap installs Docker and jq, enables Docker, creates a 2 GiB swap file and stores the AWS region under `/opt/my-fitness/region`.

## HTTPS

Caddy and Route 53 application DNS configuration are intentionally deferred until the production domain is chosen. The application container is bound only to `127.0.0.1:8080`; production traffic must eventually enter through the reverse proxy on ports 80/443.

## GitHub OIDC

This repository has GitHub immutable OIDC subjects enabled. The IAM trust policy therefore uses the immutable owner/repository identifiers plus the `main` branch ref rather than only the mutable `owner/repository` name. The CDK test asserts the exact trust condition used by the deployment role.
