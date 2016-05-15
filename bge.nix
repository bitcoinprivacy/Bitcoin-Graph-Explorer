 with import <nixpkgs> {};
 stdenv.mkDerivation {
    name = "bge";

    buildPhase = let
    sbtBootDir = "./.sbt/boot/";
    sbtIvyHome = "/var/tmp/`whoami`/.ivy";
    sbtOpts = "-XX:PermSize=190m -Dsbt.boot.directory=${sbtBootDir} -Dsbt.ivy.home=${sbtIvyHome}";
     in ''
    mkdir -p ${sbtBootDir}
    mkdir -p ${sbtIvyHome}
    sbt ${sbtOpts} assembly
    '';

    src = fetchgit {
    url = "git://github.com/bitcoinprivacy/Bitcoin-Graph-Explorer.git";
    rev = "HEAD";
    md5 = "e00fcb58959f40e1b19ee0e9e5030f23";
    } ;
    JAVA_HOME = "${jdk}";
    shellHook = ''

    export PS1="BGE > " '';
    LD_LIBRARY_PATH="${stdenv.cc.cc}/lib64";

    buildInputs = [sbt];
}
