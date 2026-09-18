# Review instructions

## Passes

Every review runs three independent passes and reports only evidence-backed findings in
changed code or its immediate behavior.

**Bugs** — behavior that can return the wrong result, lose or corrupt data, break an
existing supported flow, mishandle retries or concurrency, or leave a failure path unsafe.
Verify boundaries, defaults, error handling, and compatibility with the accepted spec.

**Security** — missing authorization at a trust boundary, injection, secret exposure,
unsafe deserialization, insecure cryptography, privilege escalation, sensitive-data
logging, or an externally reachable denial-of-service path. Treat new dependencies and
public endpoints as security-relevant.

**Compliance** — violations of declared data classification, consent, retention, audit
logging, access-control, accessibility, or regulatory controls. If no project rule
applies, state that this pass found no applicable requirement rather than inventing one.

## What Important means here

**Important** means a realistic production failure affecting correctness, security,
compliance, availability, or customer data. It blocks merge until fixed or explicitly
accepted by the responsible human.

**Minor** means a bounded issue that should be fixed before release when practical.

**Nit** means a non-blocking clarity improvement.

## Cap the nits

Report at most five Nits. Important findings are never counted against this cap and are
never suppressed by it. A review that returns two Important findings and no Nits is a
better review than one that returns twenty entries.

## Do not report

Generated code. Vendored dependencies. Formatting that the configured formatter or linter
already owns. Subjective style preferences with no correctness, security, compliance, or
maintenance consequence.
