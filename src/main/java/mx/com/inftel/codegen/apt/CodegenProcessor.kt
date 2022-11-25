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

import mx.com.inftel.codegen.apt.model.ClassModel
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes("mx.com.inftel.codegen.Codegen")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class CodegenProcessor : AbstractProcessor() {

    private val generatedClasses: MutableSet<String> = mutableSetOf()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val codegenAnnotation = processingEnv.elementUtils.getTypeElement("mx.com.inftel.codegen.Codegen")
        val annotatedClasses = roundEnv.getElementsAnnotatedWith(codegenAnnotation)
        for (annotatedClass in annotatedClasses) {
            val classModel = ClassModel(processingEnv, annotatedClass as TypeElement)
            if (classModel.isEntity && classModel.isTopLevel && classModel.isPublic && !classModel.isAbstract) {
                val packageName = classModel.packageName
                val crudQualifiedName = if (packageName.isBlank()) classModel.crudName else "${packageName}.${classModel.crudName}"
                val dtoQualifiedName = if (packageName.isBlank()) classModel.dtoName else "${packageName}.${classModel.dtoName}"
                if (!generatedClasses.contains(crudQualifiedName)) {
                    generatedClasses.add(crudQualifiedName)
                    processingEnv.filer.createSourceFile(crudQualifiedName, annotatedClass).openWriter().buffered().use { writer ->
                        writeCRUD(processingEnv, writer, classModel)
                    }
                }
                if (!generatedClasses.contains(dtoQualifiedName)) {
                    generatedClasses.add(dtoQualifiedName)
                    processingEnv.filer.createSourceFile(dtoQualifiedName, annotatedClass).openWriter().buffered().use { writer ->
                        writeDTO(processingEnv, writer, classModel)
                    }
                }
            }
        }
        return false
    }
}