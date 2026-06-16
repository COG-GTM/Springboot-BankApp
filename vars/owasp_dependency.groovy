def call(){
  // Governance-as-code gate: fail the build when a dependency has a CVE with
  // CVSS >= 7.0 (HIGH/CRITICAL), keeping the dependency baseline auditable.
  dependencyCheck additionalArguments: '--scan ./ --failOnCVSS 7 --format ALL', odcInstallation: 'OWASP'
  dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
}
