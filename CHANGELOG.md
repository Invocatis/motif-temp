## 0.1.1

Bug were mysteriously present in 0.1.0, that don't seem to be represented in the code. This is likely due to some mishandling of the clojars/git flows. Instead of overriting the clojar entry, updating a version an redeploying.

- Examples in the readme were not working as expected.

Added for new features.
Changed for changes in existing functionality.
Deprecated for soon-to-be removed features.
Removed for now removed features.
Fixed for any bug fixes.
Security in case of vulnerabilities.

## 1.0.0
### Added
- Modifier Tags for changing pattern interpretation
- Tags: Strict, star, meta, equals, use, conjunction, disjunction
- Test cases for all examples in readme
- Pretty _ function, same as clojure.core/any?

### Changed
- Moved set seqing to star modifier, now always uses disjunction
- Vectors no longer require equal sized, moved to strict vectors

## 1.0.1
### Fixed
- Added reader tag for clojurescript exception handling
