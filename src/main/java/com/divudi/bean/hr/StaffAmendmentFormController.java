/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import static com.divudi.bean.common.UtilityController.addSuccessMessage;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.AdditionalForm;
import com.divudi.entity.hr.AmendmentForm;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftHistory;
import com.divudi.facade.AmendmentFormFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "staffAmendmentFormController")
@SessionScoped
public class StaffAmendmentFormController implements Serializable {

    AmendmentForm currAmendmentForm;
    @EJB
    AmendmentFormFacade amendmentFormFacade;
    @Inject
    SessionController sessionController;
    
    @EJB
    CommonFunctions commonFunctions;
    List<AmendmentForm> amendmentForms;
    Staff fromStaff;
    Staff toStaff;
    Staff approvedStaff;
    Date fromDate;
    Date toDate;

    public StaffAmendmentFormController() {
    }

    public void clear() {
        currAmendmentForm = null;
        fromStaffShifts=null;
        toStaffShifts=null;
    }

    public boolean errorCheck() {
        if (currAmendmentForm.getFromStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }
        if (currAmendmentForm.getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Enter Staff");
            return true;
        }

        if (currAmendmentForm.getFromDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }

        if (currAmendmentForm.getToDate() == null) {
            JsfUtil.addErrorMessage("Please Select From Time");
            return true;
        }

        if (currAmendmentForm.getApprovedStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Person");
            return true;
        }
        if (currAmendmentForm.getApprovedAt() == null) {
            JsfUtil.addErrorMessage("Please Select Approved Date");
            return true;
        }
        if (currAmendmentForm.getComments() == null || "".equals(currAmendmentForm.getComments())) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }

        return false;
    }

    public void saveAmendmentForm() {
        if (errorCheck()) {
            return;
        }
        currAmendmentForm.setCreatedAt(new Date());
        currAmendmentForm.setCreater(getSessionController().getLoggedUser());
        getAmendmentFormFacade().create(currAmendmentForm);

        //CHange SHifts
        StaffShift fromStaffShift = getCurrAmendmentForm().getFromStaffShift();
        StaffShift toStaffShift = getCurrAmendmentForm().getToStaffShift();
        Staff fromStaff = getCurrAmendmentForm().getFromStaff();
        Staff toStaff = getCurrAmendmentForm().getToStaff();

        fromStaffShift.setStaff(toStaff);
        toStaffShift.setStaff(fromStaff);

        StaffShiftHistory staffShiftHistory = new StaffShiftHistory();
        staffShiftHistory.setCreatedAt(new Date());
        staffShiftHistory.setCreater(sessionController.getLoggedUser());
        staffShiftHistory.setStaff(fromStaff);
        staffShiftHistory.setStaffShift(fromStaffShift);
        staffShiftHistoryFacade.create(staffShiftHistory);

        staffShiftHistory = new StaffShiftHistory();
        staffShiftHistory.setCreatedAt(new Date());
        staffShiftHistory.setCreater(sessionController.getLoggedUser());
        staffShiftHistory.setStaff(toStaff);
        staffShiftHistory.setStaffShift(toStaffShift);
        staffShiftHistoryFacade.create(staffShiftHistory);

        staffShiftFacade.edit(fromStaffShift);
        staffShiftFacade.edit(toStaffShift);

        JsfUtil.addSuccessMessage("Sucessfully Saved");
        clear();
    }

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    List<StaffShift> fromStaffShifts;
    List<StaffShift> toStaffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;
    
    public void deleteAdditionalForm(){
        if (currAmendmentForm!=null) {
            currAmendmentForm.setRetired(true);
            currAmendmentForm.setRetirer(getSessionController().getLoggedUser());
            currAmendmentForm.setRetiredAt(new Date());
            getAmendmentFormFacade().edit(currAmendmentForm);
            JsfUtil.addSuccessMessage("Sucessfuly Deleted.");
            clear();
        } else {
            JsfUtil.addErrorMessage("Nothing to Delete.");
        }
    }
    
     public void createAmmendmentTable() {
        String sql;
        Map m = new HashMap();

        sql = " select a from AmendmentForm a where "
                + " a.createdAt between :fd and :td ";

        if (fromStaff != null) {
            sql += " and a.fromStaff=:fst ";
            m.put("st", fromStaff);
        }
        
        if (toStaff != null) {
            sql += " and a.toStaff=:tst ";
            m.put("st", toStaff);
        }

        if (approvedStaff != null) {
            sql += " and a.approvedStaff=:app ";
            m.put("app", approvedStaff);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        amendmentForms = getAmendmentFormFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }
     
     public void viewAmendmentForm(AmendmentForm amendmentForm){
         currAmendmentForm=amendmentForm;
     }

    public void fetchFromStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getFromDate());
        hm.put("stf", getCurrAmendmentForm().getFromStaff());

        fromStaffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void fetchToStaffShift() {
        HashMap hm = new HashMap();
        String sql = " select c from "
                + " StaffShift c"
                + " where c.retired=false "
                + " and c.shiftDate=:dt "
                + " and c.staff=:stf ";

        hm.put("dt", getCurrAmendmentForm().getToDate());
        hm.put("stf", getCurrAmendmentForm().getToStaff());

        toStaffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public List<StaffShift> getFromStaffShifts() {
        return fromStaffShifts;
    }

    public void setFromStaffShifts(List<StaffShift> fromStaffShifts) {
        this.fromStaffShifts = fromStaffShifts;
    }

    public List<StaffShift> getToStaffShifts() {
        return toStaffShifts;
    }

    public void setToStaffShifts(List<StaffShift> toStaffShifts) {
        this.toStaffShifts = toStaffShifts;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public AmendmentForm getCurrAmendmentForm() {
        if (currAmendmentForm == null) {
            currAmendmentForm = new AmendmentForm();
        }
        return currAmendmentForm;
    }

    public void setCurrAmendmentForm(AmendmentForm currAmendmentForm) {
        this.currAmendmentForm = currAmendmentForm;
    }

    public AmendmentFormFacade getAmendmentFormFacade() {
        return amendmentFormFacade;
    }

    public void setAmendmentFormFacade(AmendmentFormFacade amendmentFormFacade) {
        this.amendmentFormFacade = amendmentFormFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<AmendmentForm> getAmendmentForms() {
        return amendmentForms;
    }

    public void setAmendmentForms(List<AmendmentForm> amendmentForms) {
        this.amendmentForms = amendmentForms;
    }

    public Staff getApprovedStaff() {
        return approvedStaff;
    }

    public void setApprovedStaff(Staff approvedStaff) {
        this.approvedStaff = approvedStaff;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

}
