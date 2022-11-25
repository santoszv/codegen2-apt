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

import javax.lang.model.element.AnnotationMirror

class PropertyModel(
    val propertyName: String,
    val getterMethod: MethodModel?,
    val setterMethod: MethodModel?
) {

    val capitalizedName: String by lazy {
        buildString {
            append(propertyName.substring(0, 1).uppercase())
            if (propertyName.length > 1) {
                append(propertyName.substring(1))
            }
        }
    }

    val propertyType: TypeModel by lazy {
        getterMethod?.methodReturnType ?: TypeModel.Unknown
    }

    val getterName: String by lazy {
        getterMethod?.methodName ?: "get${capitalizedName}"
    }

    val setterName: String by lazy {
        setterMethod?.methodName ?: "set${capitalizedName}"
    }

    val isId: Boolean by lazy {
        getterMethod?.isId ?: false
    }

    val isVersion: Boolean by lazy {
        getterMethod?.isVersion ?: false
    }

    val isColumn: Boolean by lazy {
        getterMethod?.isColumn ?: false
    }

    val isJoinColumn: Boolean by lazy {
        getterMethod?.isJoinColumn ?: false
    }

    val isGeneratedValue: Boolean by lazy {
        getterMethod?.isGeneratedValue ?: false
    }

    val isNullable: Boolean by lazy {
        getterMethod?.isNullable ?: false
    }

    val isInsertable: Boolean by lazy {
        getterMethod?.isInsertable ?: false
    }

    val isUpdatable: Boolean by lazy {
        getterMethod?.isUpdatable ?: false
    }

    val validations: List<AnnotationMirror> by lazy {
        getterMethod?.validations ?: emptyList()
    }
}