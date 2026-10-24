# Komet Sequential Store Plugin

The sequential (32-bit) spined-array store, the store Komet used before the 64-bit spined array,
as a Komet plugin. Komet stages it into its runtime `plugins/` layer so a store on disk can still
be opened, to migrate it to the 64-bit store. Moved out of tinkar-core under
[IKE-Network/ike-issues#1313](https://github.com/IKE-Network/ike-issues/issues/1313).

<!-- BEGIN ike-managed: developer-setup -->

## Developer Setup

New to IKE development? The
[Developer Environment guide](https://ike.network/ike-tooling/ike-build-standards/developer-environment.html)
covers IDE configuration, JDK 25 setup, and the tooling conventions
every IKE workspace expects — start there before your first build.
<!-- END ike-managed: developer-setup -->
