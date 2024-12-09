/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.generator.code;

import com.dbn.generator.code.java.impl.JdbcConnectorCodeGenerator;
import com.dbn.generator.code.shared.CodeGenerator;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import com.dbn.generator.code.shared.CodeGeneratorResult;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for all code generator definitions
 * (Holds an ordered map of code {@link CodeGenerator} singletons against the {@link CodeGeneratorType} for fast lookup)
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class CodeGeneratorRegistry {
    private static final Map<CodeGeneratorType, CodeGenerator> CODE_GENERATORS = new LinkedHashMap<>();
    static {

        new JdbcConnectorCodeGenerator(CodeGeneratorType.DATABASE_CONNECTOR);
        //...
    }

    public static void register(CodeGenerator codeGenerator) {
        CODE_GENERATORS.put(codeGenerator.getType(), codeGenerator);
    }

    public static <I extends CodeGeneratorInput, R extends CodeGeneratorResult<I>> CodeGenerator<I, R> get(CodeGeneratorType type) {
        return CODE_GENERATORS.get(type);
    }

    /**
     * Lists all registered code generators
     * @return a list of {@link CodeGenerator} entities
     */
    public static List<CodeGenerator> list() {
        return new ArrayList<>(CODE_GENERATORS.values());
    }
}
