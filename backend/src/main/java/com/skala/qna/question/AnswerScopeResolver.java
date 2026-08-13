package com.skala.qna.question;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.qna.organization.Classroom;
import com.skala.qna.organization.ClassroomRepository;

@Service
@Transactional(readOnly = true)
public class AnswerScopeResolver {

	private final ClassroomRepository classrooms;

	public AnswerScopeResolver(ClassroomRepository classrooms) {
		this.classrooms = classrooms;
	}

	public Scope resolve(Question question, AnswerVisibility visibility) {
		return switch (visibility) {
		case PRIVATE -> new Scope(visibility, Set.of(question.getAuthor().getId()), Set.of());
		case CLASS -> new Scope(visibility, Set.of(), Set.of(question.getClassroom().getId()));
		case CAMPUS -> new Scope(visibility, Set.of(), classroomIds(classrooms.findAllByCampusIdOrderByName(question.getCampus().getId())));
		case GLOBAL -> new Scope(visibility, Set.of(), classroomIds(classrooms.findAll()));
		};
	}

	private Set<Long> classroomIds(Iterable<Classroom> classrooms) {
		var ids = new java.util.HashSet<Long>();
		for (var classroom : classrooms) {
			ids.add(classroom.getId());
		}
		return Set.copyOf(ids);
	}

	public record Scope(AnswerVisibility visibility, Set<Long> studentIds, Set<Long> classroomIds) {
	}
}
