(return 0 2>/dev/null) || {
    echo "Error: This script can change the PATH and PPC_CONFIG_PATH environment variables only when sourced, not executed."
    return 1
}

FPC_BUILD_PATH="$(pwd)"

export PATH="$FPC_BUILD_PATH/fpcpp/lib/fpc/3.2.2:$PATH"
export PPC_CONFIG_PATH="$FPC_BUILD_PATH/fpcpp/lib/fpc/etc"

