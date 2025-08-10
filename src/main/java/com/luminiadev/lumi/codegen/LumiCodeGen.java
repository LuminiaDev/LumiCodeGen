package com.luminiadev.lumi.codegen;

import com.luminiadev.lumi.codegen.generator.*;

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
