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
import java.io.BufferedWriter

fun writeCRUD(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("// Origin: ${classModel.qualifiedName}")
    if (classModel.packageName.isNotBlank()) {
        bufferedWriter.appendLine()
        bufferedWriter.appendLine("package ${classModel.packageName};")
    }
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("public interface ${classModel.crudName} {")
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("    jakarta.persistence.EntityManager getEntityManager();")
    writeCount(bufferedWriter, classModel)
    writeList(bufferedWriter, classModel)
    writeFind(bufferedWriter, classModel)
    writeCreate(bufferedWriter, classModel)
    writeUpdate(bufferedWriter, classModel)
    writeDelete(bufferedWriter, classModel)
    writeCountContext(bufferedWriter, classModel)
    writeListContext(bufferedWriter, classModel)
    bufferedWriter.appendLine("}")
}

private fun writeCount(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default java.lang.Long count${classModel.capitalizedName}(java.util.function.Consumer<CountContext> consumer) {""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.CriteriaQuery<java.lang.Long> criteriaQuery = criteriaBuilder.createQuery(java.lang.Long.class);""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);""")
    bufferedWriter.appendLine("""        CountContext context = new CountContext(criteriaBuilder, root);""")
    bufferedWriter.appendLine("""        consumer.accept(context);""")
    bufferedWriter.appendLine("""        criteriaQuery.select(criteriaBuilder.count(root));""")
    bufferedWriter.appendLine("""        if (!context.getPredicates().isEmpty()) {""")
    bufferedWriter.appendLine("""            criteriaQuery.where(context.getPredicates().toArray(new jakarta.persistence.criteria.Predicate[0]));""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        jakarta.persistence.TypedQuery<java.lang.Long> typedQuery = this.getEntityManager().createQuery(criteriaQuery);""")
    bufferedWriter.appendLine("""        return typedQuery.getSingleResult();""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeList(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default java.util.List<${classModel.dtoName}> list${classModel.capitalizedName}(java.util.function.Consumer<ListContext> consumer) {""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder = this.getEntityManager().getCriteriaBuilder();""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.CriteriaQuery<${classModel.qualifiedName}> criteriaQuery = criteriaBuilder.createQuery(${classModel.qualifiedName}.class);""")
    bufferedWriter.appendLine("""        jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root = criteriaQuery.from(${classModel.qualifiedName}.class);""")
    bufferedWriter.appendLine("""        ListContext context = new ListContext(criteriaBuilder, root);""")
    bufferedWriter.appendLine("""        consumer.accept(context);""")
    bufferedWriter.appendLine("""        criteriaQuery.select(root);""")
    bufferedWriter.appendLine("""        if (!context.getPredicates().isEmpty()) {""")
    bufferedWriter.appendLine("""            criteriaQuery.where(context.getPredicates().toArray(new jakarta.persistence.criteria.Predicate[0]));""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        if (!context.getOrders().isEmpty()) {""")
    bufferedWriter.appendLine("""            criteriaQuery.orderBy(context.getOrders().toArray(new jakarta.persistence.criteria.Order[0]));""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        jakarta.persistence.TypedQuery<${classModel.qualifiedName}> typedQuery = this.getEntityManager().createQuery(criteriaQuery);""")
    bufferedWriter.appendLine("""        if (context.getFirstResult() >= 0) {""")
    bufferedWriter.appendLine("""            typedQuery.setFirstResult(context.getFirstResult());""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        if (context.getMaxResults() >= 0) {""")
    bufferedWriter.appendLine("""            typedQuery.setMaxResults(context.getMaxResults());""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        if (context.getLockMode() != null) {""")
    bufferedWriter.appendLine("""            typedQuery.setLockMode(context.getLockMode());""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        return typedQuery.getResultList().stream().map(entity -> {""")
    bufferedWriter.appendLine("""            ${classModel.dtoName} data = new ${classModel.dtoName}();""")
    bufferedWriter.appendLine("""            ${classModel.dtoName}.copy2data(entity, data);""")
    bufferedWriter.appendLine("""            return data;""")
    bufferedWriter.appendLine("""        }).collect(java.util.stream.Collectors.toList());""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeFind(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    val idPropertyModel = classModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNonNullable().toCode()
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default ${classModel.dtoName} find${classModel.capitalizedName}(${idPropertyType} id) {""")
    bufferedWriter.appendLine("""        ${classModel.qualifiedName} entity = this.getEntityManager().find(${classModel.qualifiedName}.class, id);""")
    bufferedWriter.appendLine("""        if (entity == null) {""")
    bufferedWriter.appendLine("""            throw new IllegalArgumentException("Entity Not Found");""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        ${classModel.dtoName} data = new ${classModel.dtoName}();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy2data(entity, data);""")
    bufferedWriter.appendLine("""        return data;""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeCreate(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default ${classModel.dtoName} create${classModel.capitalizedName}(${classModel.dtoName} data) {""")
    bufferedWriter.appendLine("""        ${classModel.qualifiedName} entity = new ${classModel.qualifiedName}();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy4insert(this.getEntityManager(), entity, data);""")
    bufferedWriter.appendLine("""        this.getEntityManager().persist(entity);""")
    bufferedWriter.appendLine("""        this.getEntityManager().flush();""")
    bufferedWriter.appendLine("""        data = new ${classModel.dtoName}();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy2data(entity, data);""")
    bufferedWriter.appendLine("""        return data;""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeUpdate(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    val idPropertyModel = classModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNonNullable().toCode()
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default ${classModel.dtoName} update${classModel.capitalizedName}(${idPropertyType} id, ${classModel.dtoName} data) {""")
    bufferedWriter.appendLine("""        ${classModel.qualifiedName} entity = this.getEntityManager().find(${classModel.qualifiedName}.class, id);""")
    bufferedWriter.appendLine("""        if (entity == null) {""")
    bufferedWriter.appendLine("""            throw new IllegalArgumentException("Entity Not Found");""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy4update(this.getEntityManager(), entity, data);""")
    bufferedWriter.appendLine("""        this.getEntityManager().flush();""")
    bufferedWriter.appendLine("""        data = new ${classModel.dtoName}();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy2data(entity, data);""")
    bufferedWriter.appendLine("""        return data;""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeDelete(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    val idPropertyModel = classModel.idProperty ?: return
    val idPropertyType = idPropertyModel.propertyType.toNonNullable().toCode()
    bufferedWriter.appendLine()
    bufferedWriter.appendLine("""    default ${classModel.dtoName} delete${classModel.capitalizedName}(${idPropertyType} id) {""")
    bufferedWriter.appendLine("""        ${classModel.qualifiedName} entity = this.getEntityManager().find(${classModel.qualifiedName}.class, id);""")
    bufferedWriter.appendLine("""        if (entity == null) {""")
    bufferedWriter.appendLine("""            throw new IllegalArgumentException("Entity Not Found");""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""        this.getEntityManager().remove(entity);""")
    bufferedWriter.appendLine("""        this.getEntityManager().flush();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName} data = new ${classModel.dtoName}();""")
    bufferedWriter.appendLine("""        ${classModel.dtoName}.copy2data(entity, data);""")
    bufferedWriter.appendLine("""        return data;""")
    bufferedWriter.appendLine("""    }""")
}

private fun writeCountContext(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""    final class CountContext {""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        private final jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder;""")
    bufferedWriter.appendLine("""        private final jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root;""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        private final java.util.List<jakarta.persistence.criteria.Predicate> predicates;""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        CountContext(jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root) {""")
    bufferedWriter.appendLine("""            this.criteriaBuilder = criteriaBuilder;""")
    bufferedWriter.appendLine("""            this.root = root;""")
    bufferedWriter.appendLine("""            this.predicates = new java.util.ArrayList<>();""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public jakarta.persistence.criteria.CriteriaBuilder getCriteriaBuilder() {""")
    bufferedWriter.appendLine("""            return this.criteriaBuilder;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public jakarta.persistence.criteria.Root<${classModel.qualifiedName}> getRoot() {""")
    bufferedWriter.appendLine("""            return this.root;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public java.util.List<jakarta.persistence.criteria.Predicate> getPredicates() {""")
    bufferedWriter.appendLine("""            return this.predicates;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    }""")
    bufferedWriter.appendLine("""""")
}

private fun writeListContext(bufferedWriter: BufferedWriter, classModel: ClassModel) {
    bufferedWriter.appendLine("""    final class ListContext {""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        private final jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder;""")
    bufferedWriter.appendLine("""        private final jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root;""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        private int firstResult;""")
    bufferedWriter.appendLine("""        private int maxResults;""")
    bufferedWriter.appendLine("""        private jakarta.persistence.LockModeType lockMode;""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        private final java.util.List<jakarta.persistence.criteria.Predicate> predicates;""")
    bufferedWriter.appendLine("""        private final java.util.List<jakarta.persistence.criteria.Order> orders;""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        ListContext(jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, jakarta.persistence.criteria.Root<${classModel.qualifiedName}> root) {""")
    bufferedWriter.appendLine("""            this.criteriaBuilder = criteriaBuilder;""")
    bufferedWriter.appendLine("""            this.root = root;""")
    bufferedWriter.appendLine("""            this.firstResult = -1;""")
    bufferedWriter.appendLine("""            this.maxResults = -1;""")
    bufferedWriter.appendLine("""            this.lockMode = null;""")
    bufferedWriter.appendLine("""            this.predicates = new java.util.ArrayList<>();""")
    bufferedWriter.appendLine("""            this.orders = new java.util.ArrayList<>();""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public jakarta.persistence.criteria.CriteriaBuilder getCriteriaBuilder() {""")
    bufferedWriter.appendLine("""            return this.criteriaBuilder;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public int getFirstResult() {""")
    bufferedWriter.appendLine("""            return this.firstResult;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public void setFirstResult(int firstResult) {""")
    bufferedWriter.appendLine("""            this.firstResult = firstResult;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public int getMaxResults() {""")
    bufferedWriter.appendLine("""            return this.maxResults;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public void setMaxResults(int maxResults) {""")
    bufferedWriter.appendLine("""            this.maxResults = maxResults;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public jakarta.persistence.LockModeType getLockMode() {""")
    bufferedWriter.appendLine("""            return this.lockMode;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public void setLockMode(jakarta.persistence.LockModeType lockMode) {""")
    bufferedWriter.appendLine("""            this.lockMode = lockMode;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public jakarta.persistence.criteria.Root<${classModel.qualifiedName}> getRoot() {""")
    bufferedWriter.appendLine("""            return this.root;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public java.util.List<jakarta.persistence.criteria.Predicate> getPredicates() {""")
    bufferedWriter.appendLine("""            return this.predicates;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""        public java.util.List<jakarta.persistence.criteria.Order> getOrders() {""")
    bufferedWriter.appendLine("""            return this.orders;""")
    bufferedWriter.appendLine("""        }""")
    bufferedWriter.appendLine("""""")
    bufferedWriter.appendLine("""    }""")
}