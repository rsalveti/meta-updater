RPI_USE_U_BOOT_sota = "1"
PREFERRED_PROVIDER_virtual/bootloader_sota ?= "u-boot"

IMAGE_FSTYPES_remove_sota = "rpi-sdimg"
OSTREE_BOOTLOADER ?= "u-boot"

# OSTree puts its own boot.scr to bcm2835-bootfiles
IMAGE_BOOT_FILES_remove_sota += "boot.scr"
