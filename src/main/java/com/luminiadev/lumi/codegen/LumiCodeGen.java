package com.luminiadev.lumi.codegen;

public class LumiCodeGen {

    public static void main(String[] args) {
        SoundEnumGen.generate();
        ItemTypeGen.generate();
        ItemTagsGen.generate();
        ItemNamespaceIdGen.generate();
        BlockTypeGen.generate();
        BlockTagsGen.generate();
    }
}
