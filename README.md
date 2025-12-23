# BYOD 3 — CI/CD: ngrok + Jenkins + Terraform (dev)

Quick steps to expose local Jenkins with ngrok and configure GitHub webhook.

1) Start Jenkins locally (default port 8080).

2) Start ngrok (copy the HTTPS URL displayed):

```powershell
ngrok http 8080
```

3) In GitHub repository settings → Webhooks → Add webhook:
- Payload URL: `https://<your-ngrok-id>.ngrok.io/github-webhook/`
- Content type: `application/json`
- Select: `Just the push event` (or as needed)

4) In Jenkins (Credentials) add:
- AWS credential (Username/password) with ID: `AWS_CRED_ID` (username=AWS access key, password=AWS secret)
- SSH private key credential with ID: `SSH_CRED_ID`

5) Pipeline notes:
- The repository contains a `Jenkinsfile` that sets `TF_IN_AUTOMATION` and `TF_CLI_ARGS`.
- The pipeline reads the branch-specific tfvars file named `<branch>.tfvars` (e.g., `dev.tfvars`).
- The `Validate & Apply` stage appears only for the `dev` branch and requires manual approval.

6) Test flow:
- Push to any branch to run Init, Inspect, and Plan stages.
- Push to `dev` to see the manual approval step and then apply.

If your Jenkins agent is Windows-based, replace `sh` with `bat` commands in the `Jenkinsfile` or run a Linux agent.
