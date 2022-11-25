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

import java.beans.Introspector
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*

class ClassModel(
    val processingEnvironment: ProcessingEnvironment,
    val typeElement: TypeElement
) {

    val packageName: String by lazy {
        var posiblePackageElement: Element? = typeElement.enclosingElement
        while (posiblePackageElement != null && posiblePackageElement !is PackageElement) {
            posiblePackageElement = posiblePackageElement.enclosingElement
        }
        if (posiblePackageElement is PackageElement) {
            posiblePackageElement.qualifiedName.toString()
        } else {
            ""
        }
    }

    val simpleName: String by lazy {
        typeElement.simpleName.toString()
    }

    val qualifiedName: String by lazy {
        typeElement.qualifiedName.toString()
    }

    val capitalizedName: String by lazy {
        buildString {
            append(simpleName.substring(0, 1).uppercase())
            if (simpleName.length > 1) {
                append(simpleName.substring(1))
            }
        }
    }

    val isTopLevel: Boolean by lazy {
        typeElement.nestingKind == NestingKind.TOP_LEVEL
    }

    val isPublic: Boolean by lazy {
        typeElement.modifiers.contains(Modifier.PUBLIC)
    }

    val isAbstract: Boolean by lazy {
        typeElement.modifiers.contains(Modifier.ABSTRACT)
    }

    val annotations: List<AnnotationMirror> by lazy {
        processingEnvironment.elementUtils.getAllAnnotationMirrors(typeElement)
    }

    val entityAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Entity")
        }
    }

    val codegenAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.Codegen")
        }
    }

    val isEntity: Boolean by lazy {
        entityAnnotation != null
    }

    val isCodegen: Boolean by lazy {
        codegenAnnotation != null
    }

    val crudName: String by lazy {
        if (codegenAnnotation != null) {
            val crudName = processingEnvironment.elementUtils.getElementValuesWithDefaults(codegenAnnotation).filterKeys { it.simpleName.contentEquals("crud") }.values.first().value as String
            crudName.ifBlank { "${simpleName}CRUD" }
        } else {
            "${simpleName}CRUD"
        }
    }

    val dtoName: String by lazy {
        if (codegenAnnotation != null) {
            val dtoName = processingEnvironment.elementUtils.getElementValuesWithDefaults(codegenAnnotation).filterKeys { it.simpleName.contentEquals("dto") }.values.first().value as String
            dtoName.ifBlank { "${simpleName}DTO" }
        } else {
            "${simpleName}DTO"
        }
    }

    val declaredIsMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 2 && it.simpleName.startsWith("is")
        }.map {
            MethodModel(processingEnvironment, it)
        }
    }

    val declaredGetMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 3 && it.simpleName.startsWith("get")
        }.map {
            MethodModel(processingEnvironment, it)
        }
    }

    val declaredSetMethods: List<MethodModel> by lazy {
        typeElement.enclosedElements.filterIsInstance<ExecutableElement>().filter {
            !it.modifiers.contains(Modifier.STATIC)
        }.filter {
            it.simpleName.length > 3 && it.simpleName.startsWith("set")
        }.map {
            MethodModel(processingEnvironment, it)
        }
    }

    val properties: List<PropertyModel> by lazy {
        val declaredIsMethodsMap = declaredIsMethods.associateBy { Introspector.decapitalize(it.methodName.substring(2)) }
        val declaredGetMethodsMap = declaredGetMethods.associateBy { Introspector.decapitalize(it.methodName.substring(3)) }
        val declaredSetMethodsMap = declaredSetMethods.associateBy { Introspector.decapitalize(it.methodName.substring(3)) }
        val propertiesNames = (declaredIsMethodsMap.keys + declaredGetMethodsMap.keys).sorted()
        propertiesNames.map { PropertyModel(it, declaredIsMethodsMap[it] ?: declaredGetMethodsMap[it], declaredSetMethodsMap[it]) }
    }

    val idProperty: PropertyModel? by lazy {
        properties.firstOrNull { it.isId }
    }
}

