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

fun writeDTO(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("// Origin: ${classModel.qualifiedName}")
    if (classModel.packageName.isNotBlank()) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("package ${classModel.packageName};")
    }
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("public class ${classModel.dtoName} implements java.io.Serializable {")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeColumnProperty(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeJoinColumnProperty(processingEnvironment, bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeEmbeddedProperty(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    writeCopy2Data(processingEnvironment, bufferedWriter, classModel)
    writeCopy4Insert(processingEnvironment, bufferedWriter, classModel)
    writeCopy4Update(processingEnvironment, bufferedWriter, classModel)
    bufferedWriter.appendLine("}")
}

private fun writeCopy2Data(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static  void copy2data(${classModel.qualifiedName} entity, ${classModel.qualifiedDtoName} data) {""")
    for (propertyModel in classModel.properties) {
        when {
            propertyModel.isColumn -> writeAssignFromEntityColum(bufferedWriter, propertyModel)
            propertyModel.isJoinColumn -> writeAssignFromEntityJoinColum(processingEnvironment, bufferedWriter, propertyModel)
            propertyModel.isEmbedded -> writeAssignFromEmbedded(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeAssignFromEntityColum(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        data.${setterName}(entity.${getterName}());""")
}

private fun writeAssignFromEntityJoinColum(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyGetterName = idPropertyModel.getterName
    bufferedWriter.appendLine("""        if (entity.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            data.${setterName}Id(entity.${getterName}().${idPropertyGetterName}());""")
    bufferedWriter.appendLine("""        }""")
}

fun writeAssignFromEmbedded(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val embeddedClassType = embeddedClassModel.qualifiedDtoName
    bufferedWriter.appendLine("""        if (entity.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            data.${setterName}(new ${embeddedClassType}());""")
    bufferedWriter.appendLine("""            ${embeddedClassType}.copy2data(entity.${getterName}(), data.${getterName}());""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeCopy4Insert(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copy4insert(jakarta.persistence.EntityManager entityManager, ${classModel.qualifiedName} entity, ${classModel.qualifiedDtoName} data) {""")
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        val isInsertable = propertyModel.isInsertable
        val isManaged = propertyModel.isGeneratedValue || propertyModel.isVersion
        if (isInsertable && !isManaged) {
            when {
                propertyModel.isColumn -> writeAssignFromDataColum(bufferedWriter, propertyModel)
                propertyModel.isJoinColumn -> writeAssignFromDataJoinColum(processingEnvironment, bufferedWriter, index, propertyModel)
            }
        } else if (propertyModel.isEmbedded) {
            writeAssignFromDataEmbeddedInsert(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeCopy4Update(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public static void copy4update(jakarta.persistence.EntityManager entityManager, ${classModel.qualifiedName} entity, ${classModel.qualifiedDtoName} data) {""")
    for ((index, propertyModel) in classModel.properties.withIndex()) {
        val isUpdatable = propertyModel.isUpdatable
        val isManaged = propertyModel.isGeneratedValue || propertyModel.isVersion
        if (isUpdatable && !isManaged) {
            when {
                propertyModel.isColumn -> writeAssignFromDataColum(bufferedWriter, propertyModel)
                propertyModel.isJoinColumn -> writeAssignFromDataJoinColum(processingEnvironment, bufferedWriter, index, propertyModel)
            }
        } else if (propertyModel.isEmbedded) {
            writeAssignFromDataEmbeddedUpdate(processingEnvironment, bufferedWriter, propertyModel)
        }
    }
    bufferedWriter.appendLine("""    }""")
}

private fun writeAssignFromDataColum(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    bufferedWriter.appendLine("""        entity.${setterName}(data.${getterName}());""")
}

private fun writeAssignFromDataJoinColum(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, index: Int, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyType = if (propertyModel.isNullable) {
        idPropertyModel.propertyType.toNullable()
    } else {
        idPropertyModel.propertyType.toNonNullable()
    }
    if (idPropertyType is TypeModel.PrimitiveType) {
        bufferedWriter.appendLine("""        ${joinClassModel.qualifiedName} relation${index} = entityManager.find(${joinClassModel.qualifiedName}.class, data.${getterName}Id());""")
        bufferedWriter.appendLine("""        if (relation${index} == null) {""")
        bufferedWriter.appendLine("""            throw new IllegalArgumentException("Relation Not Found");""")
        bufferedWriter.appendLine("""        }""")
        bufferedWriter.appendLine("""        entity.${setterName}(relation${index});""")
    } else {
        bufferedWriter.appendLine("""        if (data.${getterName}Id() != null) {""")
        bufferedWriter.appendLine("""            ${joinClassModel.qualifiedName} relation${index} = entityManager.find(${joinClassModel.qualifiedName}.class, data.${getterName}Id());""")
        bufferedWriter.appendLine("""            if (relation${index} == null) {""")
        bufferedWriter.appendLine("""                throw new IllegalArgumentException("Relation Not Found");""")
        bufferedWriter.appendLine("""            }""")
        bufferedWriter.appendLine("""            entity.${setterName}(relation${index});""")
        bufferedWriter.appendLine("""        } else {""")
        bufferedWriter.appendLine("""            entity.${setterName}(null);""")
        bufferedWriter.appendLine("""        }""")
    }
}

fun writeAssignFromDataEmbeddedInsert(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (data.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            entity.${setterName}(new ${embeddedClassModel.qualifiedName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtoName}.copy4insert(entityManager, entity.${getterName}(), data.${getterName}());""")
    bufferedWriter.appendLine("""        }""")
}

fun writeAssignFromDataEmbeddedUpdate(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    bufferedWriter.appendLine("""        if (data.${getterName}() != null) {""")
    bufferedWriter.appendLine("""            entity.${setterName}(new ${embeddedClassModel.qualifiedName}());""")
    bufferedWriter.appendLine("""            ${embeddedClassModel.qualifiedDtoName}.copy4update(entityManager, entity.${getterName}(), data.${getterName}());""")
    bufferedWriter.appendLine("""        }""")
}

private fun writeColumnProperty(bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType.toCode()
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    private $propertyType $propertyName;""")
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $propertyType $getterName() {""")
    bufferedWriter.appendLine("""        return this.$propertyName;""")
    bufferedWriter.appendLine("""    }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void $setterName($propertyType $propertyName) {""")
    bufferedWriter.appendLine("""        this.$propertyName = $propertyName;""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeJoinColumnProperty(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val joinClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val idPropertyModel = joinClassModel.idProperty ?: return
    val idPropertyType = if (propertyModel.isNullable) {
        idPropertyModel.propertyType.toNullable().toCode()
    } else {
        idPropertyModel.propertyType.toNonNullable().toCode()
    }
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    private $idPropertyType ${propertyName}Id;""")
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $idPropertyType ${getterName}Id() {""")
    bufferedWriter.appendLine("""        return this.${propertyName}Id;""")
    bufferedWriter.appendLine("""    }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void ${setterName}Id($idPropertyType ${propertyName}Id) {""")
    bufferedWriter.appendLine("""        this.${propertyName}Id = ${propertyName}Id;""")
    bufferedWriter.appendLine("""    }""")
}

fun writeEmbeddedProperty(processingEnvironment: ProcessingEnvironment, bufferedWriter: BufferedWriter, propertyModel: PropertyModel) {
    val propertyName = propertyModel.propertyName
    val getterName = propertyModel.getterName
    val setterName = propertyModel.setterName
    val propertyType = propertyModel.propertyType as? TypeModel.ReferenceType.Class ?: return
    val embeddedClassModel = ClassModel(processingEnvironment, propertyType.declaredType.asElement() as TypeElement)
    val embeddedClassType = embeddedClassModel.qualifiedDtoName
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    private $embeddedClassType ${propertyName};""")
    bufferedWriter.appendLine("""""")
    writeValidations(bufferedWriter, propertyModel.validations)
    bufferedWriter.appendLine("""    public $embeddedClassType ${getterName}() {""")
    bufferedWriter.appendLine("""        return this.${propertyName};""")
    bufferedWriter.appendLine("""    }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    public void ${setterName}($embeddedClassType ${propertyName}) {""")
    bufferedWriter.appendLine("""        this.${propertyName} = ${propertyName};""")
    bufferedWriter.appendLine("""    }""")
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