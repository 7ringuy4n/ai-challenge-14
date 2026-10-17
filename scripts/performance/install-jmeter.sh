#!/usr/bin/env bash
set -euo pipefail

version=5.6.3
archive="apache-jmeter-${version}.tgz"
expected_sha512=5978a1a35edb5a7d428e270564ff49d2b1b257a65e17a759d259a9283fc17093e522fe46f474a043864aea6910683486340706d745fcdf3db1505fd71e689083
prefix=${JMETER_INSTALL_PREFIX:-"${HOME}/.local"}
target="${prefix}/jmeter"

if [[ -e "$target" || -L "$target" ]]; then
  if [[ ! -x "${target}/bin/jmeter" ]]; then
    printf 'Refusing to overwrite existing path without a JMeter executable: %s\n' "$target" >&2
    exit 2
  fi
  version_output=$("${target}/bin/jmeter" -v 2>&1 || true)
  installed=$(printf '%s\n' "$version_output" | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+' | tail -n1)
  if [[ "$installed" == "$version" ]]; then
    printf 'Apache JMeter %s already available at %s\n' "$version" "$target"
    exit 0
  fi
  printf 'Refusing to replace existing JMeter installation at %s (version %s). Set JMETER_INSTALL_PREFIX to another path.\n' "$target" "${installed:-UNKNOWN}" >&2
  exit 2
fi

command -v curl >/dev/null || { echo 'curl is required' >&2; exit 2; }
command -v sha512sum >/dev/null || { echo 'sha512sum is required' >&2; exit 2; }
mkdir -p "$prefix"
tmp=$(mktemp -d "${TMPDIR:-/tmp}/jmeter-${version}.XXXXXX")
trap 'rm -rf "$tmp"' EXIT
url="https://downloads.apache.org/jmeter/binaries/${archive}"
curl --fail --location --silent --show-error "$url" -o "$tmp/$archive"
printf '%s  %s\n' "$expected_sha512" "$tmp/$archive" | sha512sum --check --status
tar -xzf "$tmp/$archive" -C "$tmp"
mv "$tmp/apache-jmeter-${version}" "$target"
printf 'Installed Apache JMeter %s at %s\n' "$version" "$target"
