/*
 * Copyright 2022 Santos Zatarain Vera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mx.com.inftel.codegen.apt.model

import mx.com.inftel.codegen.apt.toJavaType
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType

sealed class TypeModel {

    abstract fun toCode(): String

    sealed class PrimitiveType : TypeModel() {

        object PrimitiveVoid : PrimitiveType() {

            override fun toCode(): String {
                return "void";
            }
        }

        object PrimitiveByte : PrimitiveType() {

            override fun toCode(): String {
                return "byte";
            }
        }

        object PrimitiveShort : PrimitiveType() {

            override fun toCode(): String {
                return "short";
            }
        }

        object PrimitiveInt : PrimitiveType() {

            override fun toCode(): String {
                return "int";
            }
        }

        object PrimitiveLong : PrimitiveType() {

            override fun toCode(): String {
                return "long";
            }
        }

        object PrimitiveFloat : PrimitiveType() {

            override fun toCode(): String {
                return "float";
            }
        }

        object PrimitiveDouble : PrimitiveType() {

            override fun toCode(): String {
                return "double";
            }
        }

        object PrimitiveBoolean : PrimitiveType() {

            override fun toCode(): String {
                return "boolean";
            }
        }

        object PrimitiveChar : PrimitiveType() {

            override fun toCode(): String {
                return "char";
            }
        }

    }

    sealed class ReferenceType : TypeModel() {

        object VoidClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Void";
            }
        }

        object ByteClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Byte";
            }
        }

        object ShortClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Short";
            }
        }

        object IntegerClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Integer";
            }
        }

        object LongClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Long";
            }
        }

        object FloatClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Float";
            }
        }

        object DoubleClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Double";
            }
        }

        object BooleanClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Boolean";
            }
        }

        object CharClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.Char";
            }
        }

        object StringClass : ReferenceType() {

            override fun toCode(): String {
                return "java.lang.String";
            }
        }

        class Array(val arrayType: ArrayType) : ReferenceType() {

            override fun toCode(): String {
                return "${arrayType.componentType.toJavaType().toCode()}[]";
            }
        }

        class Class(val declaredType: DeclaredType) : ReferenceType() {

            override fun toCode(): String {
                val typeElement = declaredType.asElement() as TypeElement
                return "${typeElement.qualifiedName}";
            }
        }

    }

    object Unknown : ReferenceType() {

        override fun toCode(): String {
            return "java.lang.Object";
        }
    }
}
