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
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class MethodModel(
     val processingEnvironment: ProcessingEnvironment,
     val executableElement: ExecutableElement
) {

    val methodName: String by lazy {
        executableElement.simpleName.toString()
    }

    val methodReturnType: TypeModel by lazy {
        executableElement.returnType.toJavaType()
    }

    val annotations: List<AnnotationMirror> by lazy {
        processingEnvironment.elementUtils.getAllAnnotationMirrors(executableElement)
    }

    val idAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Id")
        }
    }

    val generatedValueAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.GeneratedValue")
        }
    }

    val versionAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Version")
        }
    }

    val columnAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Column")
        }
    }

    val joinColumnAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.JoinColumn")
        }
    }

    val embeddedColumnAnnotation: AnnotationMirror? by lazy {
        annotations.firstOrNull {
            (it.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.persistence.Embedded")
        }
    }

    val isId: Boolean by lazy {
        idAnnotation != null
    }

    val isGeneratedValue: Boolean by lazy {
        generatedValueAnnotation != null
    }

    val isVersion: Boolean by lazy {
        versionAnnotation != null
    }

    val isColumn: Boolean by lazy {
        columnAnnotation != null
    }

    val isJoinColumn: Boolean by lazy {
        joinColumnAnnotation != null
    }

    val isEmbedded: Boolean by lazy {
        embeddedColumnAnnotation != null
    }

    val isNullable: Boolean by lazy {
        when {
            isColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(columnAnnotation!!).filterKeys { it.simpleName.contentEquals("nullable") }.values.first().value as Boolean
            isJoinColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(joinColumnAnnotation!!).filterKeys { it.simpleName.contentEquals("nullable") }.values.first().value as Boolean
            else -> false
        }
    }

    val isInsertable: Boolean by lazy {
        when {
            isColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(columnAnnotation!!).filterKeys { it.simpleName.contentEquals("insertable") }.values.first().value as Boolean
            isJoinColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(joinColumnAnnotation!!).filterKeys { it.simpleName.contentEquals("insertable") }.values.first().value as Boolean
            else -> false
        }
    }

    val isUpdatable: Boolean by lazy {
        when {
            isColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(columnAnnotation!!).filterKeys { it.simpleName.contentEquals("updatable") }.values.first().value as Boolean
            isJoinColumn -> processingEnvironment.elementUtils.getElementValuesWithDefaults(joinColumnAnnotation!!).filterKeys { it.simpleName.contentEquals("updatable") }.values.first().value as Boolean
            else -> false
        }
    }

    val validations: List<AnnotationMirror> by lazy {
        annotations.filter { ann ->
            val mirrors = ann.annotationType.asElement().annotationMirrors
            mirrors.firstOrNull { mirror ->
                (mirror.annotationType.asElement() as TypeElement).qualifiedName.contentEquals("jakarta.validation.Constraint")
            } != null
        }
    }
}