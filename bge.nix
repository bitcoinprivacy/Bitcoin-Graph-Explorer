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
        sbt ${sbtOpts} assembly publish-local
                  '';

    installPhase = ''
     mkdir -p $out
     cp  ./target/scala-2.11/bge-assembly-3.0.jar $out
     cp bge $out
    '';

    src = fetchgit {
    url = "git://github.com/bitcoinprivacy/Bitcoin-Graph-Explorer.git";
    rev = "HEAD";
    md5 = "05aa837a4581a7c1e1e5d680c0687356";
    } ;
    JAVA_HOME = "${jdk}";
    shellHook = ''

    export PS1="BGE > " '';
    LD_LIBRARY_PATH="${stdenv.cc.cc}/lib64";

    buildInputs = [sbt];
}
