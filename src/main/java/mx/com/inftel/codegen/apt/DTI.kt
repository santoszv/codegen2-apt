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
import mx.com.inftel.codegen.apt.model.PropertyModel
import mx.com.inftel.codegen.apt.model.TypeModel
import java.io.BufferedWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement

fun writeDTI(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("// Origin: ${classModel.qualifiedName}")
    if (classModel.packageName.isNotBlank()) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("package ${classModel.packageName};")
    }
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("public interface ${classModel.dtiName} {")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeColumnProperty(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeJoinColumnProperty(processingEnvironment, bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeEmbeddedProperty(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    writeCopyAllProperties(processingEnvironment, bufferedWriter, classModel)
    writeCopyInsertProperties(processingEnvironment, bufferedWriter, classModel)
    writeCopyUpdateProperties(processingEnvironment, bufferedWriter, classModel)
    writeWrapper(processingEnvironment, bufferedWriter, classModel)
    bufferedWriter.appendLine("}")
}

private fun writeWrapper(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("")
    bufferedWriter.appendLine("    interface Wrapper extends ${classModel.dtiName} {")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("        ${classModel.dtiName} getWrapped();")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeColumnDefault(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeJoinColumnDefault(processingEnvironment, bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeEmbeddedDefault(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("    }")
}

private fun writeColumnDefault(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType.toCode()
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default $propertyType $getterName() {""")
    bufferedWriter.appendLine("""            return this.getWrapped().$getterName();""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default void $setterName($propertyType $propertyName) {""")
    bufferedWriter.appendLine("""            this.getWrapped().$setterName($propertyName);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeJoinColumnDefault(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNullable().toCode()
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default $idPropertyType ${getterName}Id() {""")
    bufferedWriter.appendLine("""            return this.getWrapped().${getterName}Id();""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default void ${setterName}Id($idPropertyType ${propertyName}Id) {""")
    bufferedWriter.appendLine("""            this.getWrapped().${setterName}Id(${propertyName}Id);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeEmbeddedDefault(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val embeddedClassType = embeddedClassModel.qualifiedDtiName
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default $embeddedClassType ${getterName}() {""")
    bufferedWriter.appendLine("""            return this.getWrapped().${getterName}();""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        @java.lang.Override""")
    bufferedWriter.appendLine("""        default void ${setterName}($embeddedClassType ${propertyName}) {""")
    bufferedWriter.appendLine("""            this.getWrapped().${setterName}(${propertyName});""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeCopyAllProperties(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copyAllProperties(${classModel.qualifiedDtiName} target, ${classModel.qualifiedName} source) {""")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeAssignColumFromEntityToDti(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeAssignJoinColumFromEntityToDti(processingEnvironment, bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeAssignEmbeddedFromEntityToDti(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copyAllProperties(${classModel.qualifiedDtiName} target, ${classModel.qualifiedDtiName} source) {""")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeAssignColumFromDtiToDti(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeAssignJoinColumFromDtiToDti(bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeAssignEmbeddedFromDtiToDti(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeAssignColumFromEntityToDti(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        target.${setterName}(source.${getterName}());""")
}

private fun writeAssignJoinColumFromEntityToDti(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyGetterName = idPropertyModel.getterName
    bufferedWriter.appendLine("""        if (source.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            target.${setterName}Id(source.${getterName}().${idPropertyGetterName}());""")
    bufferedWriter.appendLine("""        } else {""")
    bufferedWriter.appendLine("""            target.${setterName}Id(null);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeAssignEmbeddedFromEntityToDti(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (source.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            target.${setterName}(new ${embeddedClassModel.qualifiedDtoName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtiName}.copyAllProperties(target.${getterName}(), source.${getterName}());""")
    bufferedWriter.appendLine("""        } else {""")
    bufferedWriter.appendLine("""            target.${setterName}(null);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeAssignColumFromDtiToDti(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        target.${setterName}(source.${getterName}());""")
}

private fun writeAssignJoinColumFromDtiToDti(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        target.${setterName}Id(source.${getterName}Id());""")
}

private fun writeAssignEmbeddedFromDtiToDti(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (source.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            target.${setterName}(new ${embeddedClassModel.qualifiedDtoName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtiName}.copyAllProperties(target.${getterName}(), source.${getterName}());""")
    bufferedWriter.appendLine("""        } else {""")
    bufferedWriter.appendLine("""            target.${setterName}(null);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeCopyInsertProperties(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copyInsertProperties(jakarta.persistence.EntityManager entityManager, ${classModel.qualifiedName} target, ${classModel.qualifiedDtiName} source) {""")
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        val isInsertable = propertyModel.isInsertable
        val isManaged = propertyModel.isGeneratedValue || propertyModel.isVersion
        if (isInsertable && !isManaged) {
            when {
                propertyModel.isColumn -> writeAssignColumFromDtiToEntity(bufferedWriter, propertyModel)
                propertyModel.isJoinColumn -> writeAssignJoinColumFromDtiFromEntity(processingEnvironment, bufferedWriter, index, propertyModel)
            }
        } else if (propertyModel.isEmbedded) {
            writeAssignEmbeddedFromDtiToEntityInsert(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeCopyUpdateProperties(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copyUpdateProperties(jakarta.persistence.EntityManager entityManager, ${classModel.qualifiedName} target, ${classModel.qualifiedDtiName} source) {""")
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        val isUpdatable = propertyModel.isUpdatable
        val isManaged = propertyModel.isGeneratedValue || propertyModel.isVersion
        if (isUpdatable && !isManaged) {
            when {
                propertyModel.isColumn -> writeAssignColumFromDtiToEntity(bufferedWriter, propertyModel)
                propertyModel.isJoinColumn -> writeAssignJoinColumFromDtiFromEntity(processingEnvironment, bufferedWriter, index, propertyModel)
            }
        } else if (propertyModel.isEmbedded) {
            writeAssignEmbeddedFromDtiToEntityUpdate(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeAssignColumFromDtiToEntity(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        target.${setterName}(source.${getterName}());""")
}

private fun writeAssignJoinColumFromDtiFromEntity(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, index: Int, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNullable()
    if (idPropertyType is TypeModel.PrimitiveType) {
        bufferedWriter.appendLine("""        ${joinClassModel.qualifiedName} relation${index} = entityManager.find(${joinClassModel.qualifiedName}.class, source.${getterName}Id());""")
        bufferedWriter.appendLine("""        if (relation${index} == null) {""")
        bufferedWriter.appendLine("""            throw new IllegalArgumentException("Relation Not Found");""")
        bufferedWriter.appendLine("""        }""")
        bufferedWriter.appendLine("""        target.${setterName}(relation${index});""")
    } else {
        bufferedWriter.appendLine("""        if (source.${getterName}Id() != null) {""")
        bufferedWriter.appendLine("""            ${joinClassModel.qualifiedName} relation${index} = entityManager.find(${joinClassModel.qualifiedName}.class, source.${getterName}Id());""")
        bufferedWriter.appendLine("""            if (relation${index} == null) {""")
        bufferedWriter.appendLine("""                throw new IllegalArgumentException("Relation Not Found");""")
        bufferedWriter.appendLine("""            }""")
        bufferedWriter.appendLine("""            target.${setterName}(relation${index});""")
        bufferedWriter.appendLine("""        } else {""")
        bufferedWriter.appendLine("""            target.${setterName}(null);""")
        bufferedWriter.appendLine("""        }""")
    }
}

private fun writeAssignEmbeddedFromDtiToEntityInsert(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (source.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            target.${setterName}(new ${embeddedClassModel.qualifiedName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtiName}.copyInsertProperties(entityManager, target.${getterName}(), source.${getterName}());""")
    bufferedWriter.appendLine("""        } else {""")
    bufferedWriter.appendLine("""            target.${setterName}(null);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeAssignEmbeddedFromDtiToEntityUpdate(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (source.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            target.${setterName}(new ${embeddedClassModel.qualifiedName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtiName}.copyUpdateProperties(entityManager, target.${getterName}(), source.${getterName}());""")
    bufferedWriter.appendLine("""        } else {""")
    bufferedWriter.appendLine("""            target.${setterName}(null);""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeColumnProperty(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType.toCode()
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $propertyType $getterName();""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void $setterName($propertyType $propertyName);""")
}

private fun writeJoinColumnProperty(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNullable().toCode()
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $idPropertyType ${getterName}Id();""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void ${setterName}Id($idPropertyType ${propertyName}Id);""")
}

private fun writeEmbeddedProperty(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val embeddedClassType = embeddedClassModel.qualifiedDtiName
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $embeddedClassType ${getterName}();""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void ${setterName}($embeddedClassType ${propertyName});""")
}

private fun writeValidations(bufferedWriter: BufferedWriter, validations: List<AnnotationMirror>) {
    for (validation in validations) {
        val annotationElement = validation.annotationType.asElement() as TypeElement
        bufferedWriter.write("    @${annotationElement.qualifiedName}")
        if (validation.elementValues.isNotEmpty()) {
            bufferedWriter.write("(")
            var first = true
            for ((key, value) in validation.elementValues) {
                if (first) {
                    first = false
                } else {
                    bufferedWriter.write(", ")
                }
                bufferedWriter.write("${key.simpleName} = $value")
            }
            bufferedWriter.write(")")
        }
        bufferedWriter.appendLine()
    }
}