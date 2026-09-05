# AWS deployment reference

These files connect the local lessons to the requested production stack. They are intentionally a reference rather than a one-command deployment: account-specific networking, DNS, certificates, an ECR repository, and an Oracle service must be supplied.

## Intended path

```text
React in browser
    └── TLS → CloudFront
                  └── TLS → Application Load Balancer
                                  └── TLS → Spring Boot on ECS Fargate
                                                   └── TCPS/TLS → Oracle Database
```

The ECS service should run in private subnets without public IP addresses. Its security group should accept traffic only from the load balancer security group. Oracle should accept TCPS traffic only from the ECS task security group.

## Files

- `cloudfront.tf` requires HTTPS from viewers and to the origin, disables caching through a supplied managed policy ID, and adds HSTS at the edge.
- `ecs-task-definition.json` is a task-definition skeleton using `awsvpc`, a non-root user, a read-only root filesystem, Secrets Manager for the database password, and an image digest placeholder.
- `kms-task-policy.json` is an example policy for the **application task role**. It limits envelope-encryption operations to one KMS key and encryption context.

Before registering the task definition:

1. Apply all three local fixes and rerun the scans.
2. Replace `LabKeyProvider` with an AWS KMS envelope-encryption implementation; the ephemeral local provider is not suitable for persistent production data.
3. Supply an Oracle TCPS JDBC URL, trust material, and a least-privilege database account.
4. Provide Spring Boot with a private TLS certificate appropriate for the load-balancer-to-task connection, or use an approved service-mesh/private-CA design.
5. Build the image, scan it, push it to ECR, and replace the placeholder with an immutable image digest.
6. Resolve every placeholder and review the generated Terraform plan and ECS definition before deployment.

Do not deploy the initial vulnerable source. The reference deliberately contains no AWS credentials or account identifiers.

