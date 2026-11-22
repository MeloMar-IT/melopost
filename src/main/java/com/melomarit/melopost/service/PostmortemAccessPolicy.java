package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;

import java.util.Set;

/** Pure authorization rules for postmortems: department scope and the Published lock. */
public final class PostmortemAccessPolicy {

    public enum Action { VIEW, CREATE, UPDATE, DELETE }

    /** Who is asking: unrestricted (admin / internal call) or limited to a set of departments. */
    public record Subject(boolean unrestricted, Set<String> departments) {
        public static final Subject UNRESTRICTED = new Subject(true, Set.of());
    }

    private PostmortemAccessPolicy() {
    }

    public static boolean canAccessDepartment(Subject subject, String department) {
        if (subject.unrestricted()) {
            return true;
        }
        return department != null && !department.isBlank() && subject.departments().contains(department);
    }

    public static void check(Subject subject, Postmortem pm, Action action) {
        if (subject.unrestricted()) {
            return;
        }
        if (!canAccessDepartment(subject, pm.getDepartment())) {
            throw new PostmortemAccessDeniedException("Access denied: you are not allowed to access postmortems of this department");
        }
        if ((action == Action.UPDATE || action == Action.DELETE) && "Published".equals(pm.getStatus())) {
            throw new PostmortemLockedException(pm.getUuid());
        }
    }
}
