SUMMARY = "Aktualizr SOTA Client"
DESCRIPTION = "SOTA Client application written in C++"
HOMEPAGE = "https://github.com/advancedtelematic/aktualizr"
SECTION = "base"
LICENSE = "MPL-2.0"
LIC_FILES_CHKSUM = "file://${S}/LICENSE;md5=9741c346eef56131163e13b9db1241b3"

DEPENDS = "boost curl openssl libarchive libsodium asn1c-native "
DEPENDS_append_class-target = "jansson ostree ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'hsm', ' libp11', '', d)} "
DEPENDS_append_class-native = "glib-2.0-native "

RDEPENDS_${PN}_class-target = "lshw "
RDEPENDS_${PN}_append_class-target = "${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'hsm', ' engine-pkcs11', '', d)} "
RDEPENDS_${PN}_append_class-target = " ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'serialcan', '  slcand-start', '', d)} "

PV = "1.0+git${SRCPV}"
PR = "7"

SRC_URI = " \
  gitsm://github.com/advancedtelematic/aktualizr;branch=${BRANCH} \
  file://0001-Move-default-sota.toml-from-usr-lib-to-etc.patch \
  file://0001-garage-check-Handle-n-s-in-tufrepo.url.patch \
  file://aktualizr.service \
  file://aktualizr-serialcan.service \
  "
SRCREV = "32a04532c1b7434b9cab89b78df389059ac45d40"
BRANCH ?= "master"

S = "${WORKDIR}/git"

inherit cmake

inherit systemd
SYSTEMD_SERVICE_${PN} = "aktualizr.service"

BBCLASSEXTEND =+ "native"

EXTRA_OECMAKE = "-DWARNING_AS_ERROR=OFF -DCMAKE_BUILD_TYPE=Release -DAKTUALIZR_VERSION=${PV} "
EXTRA_OECMAKE_append_class-target = " -DBUILD_OSTREE=ON -DBUILD_ISOTP=ON ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'hsm', '-DBUILD_P11=ON', '', d)} "
EXTRA_OECMAKE_append_class-native = " -DBUILD_SOTA_TOOLS=ON -DBUILD_OSTREE=OFF "

export SOTA_LEGACY_SECONDARY_INTERFACE
export SOTA_VIRTUAL_SECONDARIES

do_install_append () {
    rm -f ${D}${bindir}/aktualizr_cert_provider
    rm -fr ${D}${libdir}/systemd

    install -d ${D}${libdir}/sota
    install -m 0644 ${S}/config/sota_autoprov.toml ${D}/${libdir}/sota/sota_autoprov.toml
    install -m 0644 ${S}/config/sota_hsm_prov.toml ${D}/${libdir}/sota/sota_hsm_prov.toml
    install -m 0644 ${S}/config/sota_implicit_prov.toml ${D}/${libdir}/sota/sota_implicit_prov.toml
}
do_install_append_class-target () {
    rm -f ${D}${libdir}/sota/sota.toml
    ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'secondary-example', '', 'rm -f ${D}${bindir}/example-interface', d)}
    ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'secondary-isotp-example', '', 'rm -f ${D}${bindir}/isotp-test-interface', d)}

    install -d ${D}${systemd_unitdir}/system
    aktualizr_service=${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'serialcan', '${WORKDIR}/aktualizr-serialcan.service', '${WORKDIR}/aktualizr.service', d)}
    install -m 0644 ${aktualizr_service} ${D}${systemd_unitdir}/system/aktualizr.service

    # Create default environment config file
    if [ -n "${SOTA_LEGACY_SECONDARY_INTERFACE}" ]; then
        AKTUALIZR_PARAMETERS_LEGACYSEC="--legacy-interface ${SOTA_LEGACY_SECONDARY_INTERFACE}";
    fi

    AKTUALIZR_PARAMETERS_CONFIGFILE="--config ${localstatedir}/sota/sota.toml"
    for sec in ${SOTA_VIRTUAL_SECONDARIES}; do
        AKTUALIZR_PARAMETERS_VIRTUALSECS="${AKTUALIZR_PARAMETERS_VIRTUALSECS} --secondary-config $sec"
    done

    install -d ${D}${sysconfdir}/sota
    echo "AKTUALIZR_CMDLINE_PARAMETERS=${AKTUALIZR_PARAMETERS_CONFIGFILE} ${AKTUALIZR_PARAMETERS_LEGACYSEC} ${AKTUALIZR_PARAMETERS_VIRTUALSECS}" > ${D}${sysconfdir}/sota/sota.env
}
do_install_append_class-native () {
    rm -f ${D}${bindir}/aktualizr
    rm -f ${D}${bindir}/aktualizr-info
    rm -f ${D}${bindir}/example-interface

    install -m 0755 ${B}/src/sota_tools/garage-sign-prefix/src/garage-sign/bin/* ${D}${bindir}
    install -m 0644 ${B}/src/sota_tools/garage-sign-prefix/src/garage-sign/lib/* ${D}${libdir}
}

FILES_${PN}_class-target = " \
                ${bindir}/aktualizr \
                ${bindir}/aktualizr-info \
                ${sysconfdir}/sota/sota.env \
                ${systemd_unitdir}/system/aktualizr.service \
                ${libdir}/sota/schemas \
                "
FILES_${PN}_append_class-target = " ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'secondary-example', ' ${bindir}/example-interface', '', d)} "
FILES_${PN}_append_class-target = " ${@bb.utils.contains('SOTA_CLIENT_FEATURES', 'secondary-isotp-example', ' ${bindir}/isotp-test-interface', '', d)} "

FILES_${PN}_class-native = " \
                ${bindir}/aktualizr_implicit_writer \
                ${bindir}/garage-deploy \
                ${bindir}/garage-push \
                ${libdir}/sota \
                "

PACKAGES_append_class-target = " ${PN}-extras"
FILES_${PN}-extras_class-target = " \
                ${bindir}/aktualizr_implicit_writer \
                ${libdir}/sota/sota_autoprov.toml \
                ${libdir}/sota/sota_hsm_prov.toml \
                ${libdir}/sota/sota_implicit_prov.toml \
                "

# vim:set ts=4 sw=4 sts=4 expandtab:
