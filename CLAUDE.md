# komet-sequential-store-plugin

The sequential (32-bit) spined-array store as a Komet plugin. It left tinkar-core under
IKE-Network/ike-issues#1313: the module `dev.ikm.tinkar.provider.spinedarray` (once
`tinkar-core/provider/data-spinedarray-provider`) with the spined maps it kept its data in (once
tinkar-core's `collection` module, now the unexported `...spinedarray.collection` packages).

The 64-bit spined array (`data-spinedarray64-provider` in tinkar-core) carries the memory-only and
persistent roles. This store remains so a store on disk can be opened, to migrate it; once no
store needs it, the plugin is dropped. Design and plan: `~/ike-dev/notes/2026-10-21-memory-only-store-switch.adoc`,
section "Moving the sequential store into an external plugin".

## Key Facts

- GroupId: `network.ike.komet`; version `1-SNAPSHOT`; ike-parent 206; komet-bom for tinkar versions
- Controllers: "Open SpinedArrayStore", "New SpinedArrayStore", and "Load Ephemeral Store"
  (retired, hidden by Komet's chooser; kept for the store's own tests)
- The module keeps its tinkar-core name: `dev.ikm.tinkar.common` exports
  `dev.ikm.tinkar.common.service.internal` to it by name
- Komet stages the jar into its runtime `plugins/` layer (komet-desktop `pom.xml`, beside
  rocks-kb-engine). ike-knowledge-provider and tinkar-service take it at runtime scope, for the
  published `reasoned-sa` stores
- Tests run on the class path (ike-parent's `useModulePath=false`); the `*IT` suites load the IKE
  starter set from `target/data`

## Build

Inside a workspace, build from the workspace root: `./mvnw -pl komet-sequential-store-plugin ...`
there, not from this directory (IKE-Network/ike-issues#1265).

<!-- BEGIN ike-managed: standards-pointer -->

## IKE Build Standards

This project follows the IKE build standards. Run `mvn validate` to
unpack them into `.claude/standards/` — build artifacts from
`ike-build-standards`, so **do not edit or commit them** — then read and
follow them (start with `MAVEN.md` and `IKE-MAVEN.md`).

Diagrams on web pages (`src/site/asciidoc/`) follow `IKE-DIAGRAMS.md`:
pre-render to committed static SVG under `src/site/resources/images/` and
reference with `image::` — never inline `[plantuml]`/`[graphviz]` blocks
or live Kroki URLs (the Maven site parser does not render them).
<!-- END ike-managed: standards-pointer -->
