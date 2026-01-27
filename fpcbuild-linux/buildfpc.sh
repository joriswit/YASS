if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME is not set. Please export it before running this script." >&2
    exit 1
fi

export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/16.1.4479499"

FPC_BUILD_PATH="$(pwd)"

FPC_CFG_FILE="$FPC_BUILD_PATH/fpc.cfg"

if [ ! -f "$FPC_CFG_FILE" ]; then
    echo "Error: File '$FPC_CFG_FILE' does not exist." >&2
    exit 1
fi

# Get the fpc 3.2.2 sources
git clone --single-branch --branch release_3_2_2 --depth 1 https://gitlab.com/freepascal.org/fpc/source.git fpcsrc

cd fpcsrc

# Change version date to same date as official win32 build
sed -i -e "s/.\$I\s.DATE../\'2021\/05\/16\'/g" compiler/version.pas

build_target() {
    local cpu_target=$1
    local binutils_prefix=$2
    local toolchain_dir=$3

    if make crossall crossinstall \
        OS_TARGET=android \
        CPU_TARGET=$cpu_target \
        CROSSOPT="@$FPC_CFG_FILE" \
        CROSSBINDIR="$ANDROID_NDK_HOME/toolchains/$toolchain_dir/prebuilt/linux-x86_64/bin" \
        BINUTILSPREFIX=$binutils_prefix \
        INSTALL_PREFIX="$FPC_BUILD_PATH/fpcpp"; then
        echo "Build for $cpu_target succeeded."
    else
        echo "Build for $cpu_target failed. Aborting."
        exit 1
    fi
}

#  Build for each architecture
build_target arm arm-linux-androideabi- arm-linux-androideabi-4.9
build_target i386 i686-linux-android- x86-4.9
build_target aarch64 aarch64-linux-android- aarch64-linux-android-4.9
build_target x86_64 x86_64-linux-android- x86_64-4.9

cd $FPC_BUILD_PATH

# Copy fpc.cfg file, and add line for unit path
mkdir $FPC_BUILD_PATH/fpcpp/lib/fpc/etc
cp $FPC_CFG_FILE $FPC_BUILD_PATH/fpcpp/lib/fpc/etc/fpc.cfg

echo "-Fu$FPC_BUILD_PATH/fpcpp/lib/fpc/3.2.2/units/\$fpctarget/*" >> $FPC_BUILD_PATH/fpcpp/lib/fpc/etc/fpc.cfg

