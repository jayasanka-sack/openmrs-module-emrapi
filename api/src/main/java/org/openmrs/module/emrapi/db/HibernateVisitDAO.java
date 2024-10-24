package org.openmrs.module.emrapi.db;

import org.hibernate.SessionFactory;
import org.openmrs.Diagnosis;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.module.emrapi.visit.VisitWithDiagnoses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class HibernateVisitDAO implements VisitDAO {

    @Autowired
    private SessionFactory sessionFactory;

    public List<VisitWithDiagnoses> getVisitsByPatientId(Patient patient, int startIndex, int limit) {

        String visitNoteEncounterTypeUuid = "d7151f82-c1f3-4152-a605-2f9ea7414a79";


        String hqlVisit="SELECT DISTINCT v FROM Visit v " +
                "JOIN FETCH v.encounters enc " +
                "JOIN enc.encounterType et " +
                "WHERE v.patient.id = :patientId " +
                "AND et.uuid = :encounterTypeUuid " +
                "ORDER BY v.startDatetime DESC "+
                "LIMIT :startIndex, :limit";

         List<Visit> visits = sessionFactory.getCurrentSession()
                .createQuery(hqlVisit)
                .setParameter("patientId", patient.getId())
                .setParameter("encounterTypeUuid", visitNoteEncounterTypeUuid).list();

        String hqlDiagnosis = "SELECT DISTINCT diag FROM Diagnosis diag " +
                "JOIN diag.encounter e " +
                "WHERE e.visit.id IN :visitIds";

        List<Integer> visitIds = visits.stream()
                .map(Visit::getId)
                .collect(Collectors.toList());

        List<Diagnosis> diagnoses = sessionFactory.getCurrentSession()
                .createQuery(hqlDiagnosis)
                .setParameterList("visitIds", visitIds)
                .list();

        Map<Visit, Set<Diagnosis>> visitToDiagnosesMap = new HashMap<>();
        for (Diagnosis diagnosis : diagnoses) {
            Visit visit = diagnosis.getEncounter().getVisit();
            visitToDiagnosesMap
                    .computeIfAbsent(visit, v -> new HashSet<>())
                    .add(diagnosis);
        }

        List<VisitWithDiagnoses> visitWithDiagnoses = visits.stream()
                .sorted(Comparator.comparing(Visit::getStartDatetime).reversed())
                .map(visit -> new VisitWithDiagnoses(visit, visitToDiagnosesMap.getOrDefault(visit, new HashSet<>())))
                .collect(Collectors.toList());

        return visitWithDiagnoses;
    }
}