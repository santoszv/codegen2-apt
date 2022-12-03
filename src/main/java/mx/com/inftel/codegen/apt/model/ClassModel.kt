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

    val codegenAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("mx.com.inftel.codegen.Codegen")
        }
    }

    val entityAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Entity")
        }
    }

    val embeddableAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Embeddable")
        }
    }

    val isCodegen: Boolean by lazy {
        codegenAnnotation != null
    }

    val isEntity: Boolean by lazy {
        entityAnnotation != null
    }

    val isEmbeddable: Boolean by lazy {
        embeddableAnnotation != null
    }

    val crudName: String by lazy {
        if (codegenAnnotation != null) {
            val crudName = processingEnvironment.elementUtils.getElementValuesWithDefaults(codegenAnnotation).filterKeys { it.simpleName.contentEquals("crud") }.values.firstOrNull()?.value as? String ?: ""
            crudName.ifBlank { "${simpleName}CRUD" }
        } else {
            "${simpleName}CRUD"
        }
    }

    val dtoName: String by lazy {
        if (codegenAnnotation != null) {
            val dtoName = processingEnvironment.elementUtils.getElementValuesWithDefaults(codegenAnnotation).filterKeys { it.simpleName.contentEquals("dto") }.values.firstOrNull()?.value as? String ?: ""
            dtoName.ifBlank { "${simpleName}DTO" }
        } else {
            "${simpleName}DTO"
        }
    }

    val qualifiedCrudName: String by lazy {
        if (packageName.isBlank()) {
            crudName
        } else {
            "${packageName}.${crudName}"
        }
    }

    val qualifiedDtoName: String by lazy {
        if (packageName.isBlank()) {
            dtoName
        } else {
            "${packageName}.${dtoName}"
        }
    }

    val allMethods: List<ExecutableElement> by lazy {
        processingEnvironment.elementUtils.getAllMembers(typeElement).filterIsInstance<ExecutableElement>()
    }

    val allPropertiesNames: List<String> by lazy {
        val properties: MutableSet<String> = mutableSetOf()
        for (method in allMethods) {
            if (!method.modifiers.contains(Modifier.STATIC)) {
                if (method.simpleName.length > 3 && method.simpleName.startsWith("get")) {
                    properties.add(Introspector.decapitalize(method.simpleName.substring(3)))
                } else if (method.simpleName.length > 2 && method.simpleName.startsWith("is")) {
                    properties.add(Introspector.decapitalize(method.simpleName.substring(2)))
                }
            }
        }
        properties.toList()
    }

    val allGettersElements: Map<String, ExecutableElement> by lazy {
        val getters: MutableMap<String, ExecutableElement> = mutableMapOf()
        for (method in allMethods) {
            if (!method.modifiers.contains(Modifier.STATIC)) {
                if (method.simpleName.length > 3 && method.simpleName.startsWith("get")) {
                    getters[Introspector.decapitalize(method.simpleName.substring(3))] = method
                } else if (method.simpleName.length > 2 && method.simpleName.startsWith("is")) {
                    getters[Introspector.decapitalize(method.simpleName.substring(2))] = method
                }
            }
        }
        getters.toMap()
    }

    val allSettersElements: Map<String, ExecutableElement> by lazy {
        val setters: MutableMap<String, ExecutableElement> = mutableMapOf()
        for (method in allMethods) {
            if (!method.modifiers.contains(Modifier.STATIC)) {
                if (method.simpleName.length > 3 && method.simpleName.startsWith("set")) {
                    setters[Introspector.decapitalize(method.simpleName.substring(3))] = method
                }
            }
        }
        setters.toMap()
    }

    val properties: List<PropertyModel> by lazy {
        val properties: MutableList<PropertyModel> = mutableListOf()
        for (propertyName in allPropertiesNames) {
            val getter = allGettersElements[propertyName]
            val setter = allSettersElements[propertyName]
            if (getter != null && setter != null) {
                properties.add(PropertyModel(propertyName, MethodModel(processingEnvironment, getter), MethodModel(processingEnvironment, setter)))
            }
        }
        properties.toList()
    }

    val idProperty: PropertyModel? by lazy {
        properties.firstOrNull { it.isId }
    }
}

