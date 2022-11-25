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

package mx.com.inftel.codegen.apt

import mx.com.inftel.codegen.apt.model.TypeModel
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

fun TypeMirror.toJavaType(): TypeModel {
    return when (this.kind) {
        TypeKind.BOOLEAN -> TypeModel.PrimitiveType.PrimitiveBoolean
        TypeKind.BYTE -> TypeModel.PrimitiveType.PrimitiveByte
        TypeKind.SHORT -> TypeModel.PrimitiveType.PrimitiveShort
        TypeKind.INT -> TypeModel.PrimitiveType.PrimitiveInt
        TypeKind.LONG -> TypeModel.PrimitiveType.PrimitiveLong
        TypeKind.CHAR -> TypeModel.PrimitiveType.PrimitiveChar
        TypeKind.FLOAT -> TypeModel.PrimitiveType.PrimitiveFloat
        TypeKind.DOUBLE -> TypeModel.PrimitiveType.PrimitiveDouble
        TypeKind.VOID -> TypeModel.PrimitiveType.PrimitiveVoid
        TypeKind.ARRAY -> TypeModel.ReferenceType.Array(this as ArrayType)
        TypeKind.DECLARED -> (this as DeclaredType).toJavaType()
        else -> TypeModel.Unknown
    }
}

fun DeclaredType.toJavaType(): TypeModel {
    return when ((this.asElement() as TypeElement).qualifiedName.toString()) {
        "java.lang.Boolean" -> TypeModel.ReferenceType.BooleanClass
        "java.lang.Byte" -> TypeModel.ReferenceType.ByteClass
        "java.lang.Short" -> TypeModel.ReferenceType.ShortClass
        "java.lang.Integer" -> TypeModel.ReferenceType.IntegerClass
        "java.lang.Long" -> TypeModel.ReferenceType.LongClass
        "java.lang.Char" -> TypeModel.ReferenceType.CharClass
        "java.lang.Float" -> TypeModel.ReferenceType.FloatClass
        "java.lang.Double" -> TypeModel.ReferenceType.DoubleClass
        "java.lang.Void" -> TypeModel.ReferenceType.VoidClass
        "java.lang.String" -> TypeModel.ReferenceType.StringClass
        else -> TypeModel.ReferenceType.Class(this)
    }
}

fun TypeModel.toNullable(): TypeModel {
    return when (this) {
        TypeModel.PrimitiveType.PrimitiveBoolean -> TypeModel.ReferenceType.BooleanClass
        TypeModel.PrimitiveType.PrimitiveByte -> TypeModel.ReferenceType.ByteClass
        TypeModel.PrimitiveType.PrimitiveShort -> TypeModel.ReferenceType.ShortClass
        TypeModel.PrimitiveType.PrimitiveInt -> TypeModel.ReferenceType.IntegerClass
        TypeModel.PrimitiveType.PrimitiveLong -> TypeModel.ReferenceType.LongClass
        TypeModel.PrimitiveType.PrimitiveChar -> TypeModel.ReferenceType.CharClass
        TypeModel.PrimitiveType.PrimitiveFloat -> TypeModel.ReferenceType.FloatClass
        TypeModel.PrimitiveType.PrimitiveDouble -> TypeModel.ReferenceType.DoubleClass
        TypeModel.PrimitiveType.PrimitiveVoid -> TypeModel.ReferenceType.VoidClass
        else -> this
    }
}

fun TypeModel.toNonNullable(): TypeModel {
    return when (this) {
        TypeModel.ReferenceType.BooleanClass -> TypeModel.PrimitiveType.PrimitiveBoolean
        TypeModel.ReferenceType.ByteClass -> TypeModel.PrimitiveType.PrimitiveByte
        TypeModel.ReferenceType.ShortClass -> TypeModel.PrimitiveType.PrimitiveShort
        TypeModel.ReferenceType.IntegerClass -> TypeModel.PrimitiveType.PrimitiveInt
        TypeModel.ReferenceType.LongClass -> TypeModel.PrimitiveType.PrimitiveLong
        TypeModel.ReferenceType.CharClass -> TypeModel.PrimitiveType.PrimitiveChar
        TypeModel.ReferenceType.FloatClass -> TypeModel.PrimitiveType.PrimitiveFloat
        TypeModel.ReferenceType.DoubleClass -> TypeModel.PrimitiveType.PrimitiveDouble
        TypeModel.ReferenceType.VoidClass -> TypeModel.PrimitiveType.PrimitiveVoid
        else -> this
    }
}