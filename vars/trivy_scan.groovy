def call(){
  // Governance-as-code gate: fail the build on new HIGH/CRITICAL findings.
  // Scan the built artifact (target/*.jar) so the managed transitive dependency
  // tree is resolved accurately. Explicitly-waived items live in .trivyignore.yaml.
  // --ignore-unfixed avoids noise from vulnerabilities without an available patch.
  sh "trivy fs --quiet --format table --output trivy-fs-report.txt ."
  sh "trivy rootfs --quiet --exit-code 1 --severity HIGH,CRITICAL --ignore-unfixed --ignorefile .trivyignore.yaml target/"
  sh "trivy config --quiet --exit-code 1 --severity HIGH,CRITICAL --ignorefile .trivyignore.yaml ."
}
