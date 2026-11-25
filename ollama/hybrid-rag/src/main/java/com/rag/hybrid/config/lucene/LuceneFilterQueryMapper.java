package com.rag.hybrid.config.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.Group;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.Filter.Operand;
import org.springframework.ai.vectorstore.filter.Filter.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@NullMarked
public class LuceneFilterQueryMapper {

    /**
     * Converts a Spring AI Filter.Expression into a Lucene Query. Returns MatchAllDocsQuery if no
     * filter is provided so searches work seamlessly without filters.
     */
    public Query toLuceneQuery(@Nullable Expression expression) {
        return expression == null ? MatchAllDocsQuery.INSTANCE : mapOperand(expression);
    }

    private Query mapOperand(Operand operand) {
        return switch (operand) {
            case Group(Operand content) -> mapOperand(content);
            case Expression expr -> mapExpression(expr);
            default -> throw new IllegalArgumentException("Unsupported filter operand: " + operand);
        };
    }

    private Query mapExpression(Expression expr) {
        return switch (expr.type()) {
            case EQ -> buildTermQuery(expr);
            case NE -> negate(buildTermQuery(expr));
            case IN -> buildInQuery(expr);
            case NIN -> negate(buildInQuery(expr));
            case NOT ->
                    negate(
                            mapOperand(
                                    Objects.requireNonNull(
                                            expr.left(), "NOT operand must not be null")));
            case AND -> combine(expr, BooleanClause.Occur.FILTER, BooleanClause.Occur.FILTER);
            case OR -> combine(expr, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD);
            default ->
                    throw new UnsupportedOperationException(
                            "Unsupported filter operator: " + expr.type());
        };
    }

    private Query buildTermQuery(Expression expr) {
        return switch (expr) {
            case Expression(_, Key(String field), Value(String val)) when !field.isBlank() ->
                    new TermQuery(new Term(field, val));
            default -> throw new IllegalArgumentException("Invalid equality expression: " + expr);
        };
    }

    private Query buildInQuery(Expression expr) {
        return switch (expr) {
            case Expression(_, Key(String field), Value(List<?> values))
                    when !field.isBlank() && !values.isEmpty() -> {
                List<BytesRef> terms =
                        values.stream()
                                .filter(Objects::nonNull)
                                .map(value -> new BytesRef(value.toString()))
                                .toList();
                yield new TermInSetQuery(field, terms);
            }
            default -> throw new IllegalArgumentException("Invalid IN/NIN expression: " + expr);
        };
    }

    private Query negate(Query query) {
        return new BooleanQuery.Builder()
                .add(MatchAllDocsQuery.INSTANCE, BooleanClause.Occur.MUST)
                .add(query, BooleanClause.Occur.MUST_NOT)
                .build();
    }

    private Query combine(
            Expression expr, BooleanClause.Occur leftOccur, BooleanClause.Occur rightOccur) {
        var builder =
                new BooleanQuery.Builder()
                        .add(mapOperand(Objects.requireNonNull(expr.left())), leftOccur)
                        .add(mapOperand(Objects.requireNonNull(expr.right())), rightOccur);

        if (leftOccur == BooleanClause.Occur.SHOULD) {
            builder.setMinimumNumberShouldMatch(1);
        }
        return builder.build();
    }
}
