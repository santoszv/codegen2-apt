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
                if (!generatedClasses.contains(classModel.qualifiedCrudName)) {
                    generatedClasses.add(classModel.qualifiedCrudName)
                    processingEnv.filer.createSourceFile(classModel.qualifiedCrudName, annotatedClass).openWriter().buffered().use { writer ->
                        writeCRUD(writer, classModel)
                    }
                }
                if (!generatedClasses.contains(classModel.qualifiedDtoName)) {
                    generatedClasses.add(classModel.qualifiedDtoName)
                    processingEnv.filer.createSourceFile(classModel.qualifiedDtoName, annotatedClass).openWriter().buffered().use { writer ->
                        writeDTO(processingEnv, writer, classModel)
                    }
                }
                if (!generatedClasses.contains(classModel.qualifiedDtiName)) {
                    generatedClasses.add(classModel.qualifiedDtiName)
                    processingEnv.filer.createSourceFile(classModel.qualifiedDtiName, annotatedClass).openWriter().buffered().use { writer ->
                        writeDTI(processingEnv, writer, classModel)
                    }
                }
            }
            if (classModel.isEmbeddable && classModel.isTopLevel && classModel.isPublic && !classModel.isAbstract) {
                if (!generatedClasses.contains(classModel.qualifiedDtoName)) {
                    generatedClasses.add(classModel.qualifiedDtoName)
                    processingEnv.filer.createSourceFile(classModel.qualifiedDtoName, annotatedClass).openWriter().buffered().use { writer ->
                        writeDTO(processingEnv, writer, classModel)
                    }
                }
                if (!generatedClasses.contains(classModel.qualifiedDtiName)) {
                    generatedClasses.add(classModel.qualifiedDtiName)
                    processingEnv.filer.createSourceFile(classModel.qualifiedDtiName, annotatedClass).openWriter().buffered().use { writer ->
                        writeDTI(processingEnv, writer, classModel)
                    }
                }
            }
        }
        return false
    }
}