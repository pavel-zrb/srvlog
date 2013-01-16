package com.payneteasy.srvlog.wicket.page;

import com.payneteasy.srvlog.data.*;
import com.payneteasy.srvlog.service.ILogCollector;
import com.payneteasy.srvlog.service.IndexerServiceException;
import com.payneteasy.srvlog.util.DateRange;
import com.payneteasy.srvlog.util.DateRangeType;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.util.*;

import static com.payneteasy.srvlog.wicket.component.repeater.LogDataTableUtil.setHighlightCssClass;

/**
 * Date: 11.01.13
 */
public class LogMonitorPage extends BasePage {

    public LogMonitorPage(PageParameters pageParameters) {
        super(pageParameters, LogMonitorPage.class);

        FeedbackPanel feedbackPanel = new FeedbackPanel("feedback-panel");
        add(feedbackPanel);

        final FilterModel filterModel = new FilterModel();

        Form<FilterModel> form = new Form<FilterModel>("form");
        add(form);

        final DropDownChoice<DateRangeType> dateRangeType = new DropDownChoice<DateRangeType>(
                "date-range-type"
                , new PropertyModel<DateRangeType>(filterModel, "dateRangeType") {
            @Override
            public void setObject(DateRangeType object) {
                super.setObject(object);    //To change body of overridden methods use File | Settings | File Templates.
            }
        }
                , Arrays.asList(DateRangeType.values())
                , new IChoiceRenderer<DateRangeType>() {
            @Override
            public Object getDisplayValue(DateRangeType object) {
                return object.name();
            }

            @Override
            public String getIdValue(DateRangeType object, int index) {
                return object.name();
            }
        });
        form.add(dateRangeType);
        dateRangeType.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
//                if(isVisibleDateField(filterModel.getDateRangeType())){
//                    holderDateRangeContainer.setVisible(true);
//                    target.add(holderDateRangeContainer);
//                }else {
//                    holderDateRangeContainer.setVisible(false);
//                    target.add(holderDateRangeContainer);
//                }
                dateRangeType.onSelectionChanged();

                target.add(holderDateRangeContainer);
            }
        });

        holderDateRangeContainer = new WebMarkupContainer("holder-exactly-dateRange") {

            @Override
            public boolean isVisible() {
                return isVisibleDateField(filterModel.getDateRangeType());    //To change body of overridden methods use File | Settings | File Templates.
            }
        };
        holderDateRangeContainer.setOutputMarkupPlaceholderTag(true);
        //holderDateRangeContainer.setVisible(isVisibleDateField(filterModel.getDateRangeType()));
        form.add(holderDateRangeContainer);

        dateFromTextField = new DateTextField("dateFrom-field", new PropertyModel<Date>(filterModel, "exactlyDateFrom"), "dd.MM.yyyy");
        dateFromTextField.add(new DatePicker());
        holderDateRangeContainer.add(dateFromTextField);

        dateToTextField = new DateTextField("dateTo-field", new PropertyModel<Date>(filterModel, "exactlyDateTo"), "dd.MM.yyyy");
        dateToTextField.add(new DatePicker());
        holderDateRangeContainer.add(dateToTextField);



        ListMultipleChoice<LogLevel> severityChoice = new ListMultipleChoice<LogLevel>(
                "severity-choice"
                , new PropertyModel<List<LogLevel>>(filterModel, "severities")
                , LogLevel.getLogEnumList(), new IChoiceRenderer<LogLevel>() {
            @Override
            public Object getDisplayValue(LogLevel object) {
                return object.name();
            }

            @Override
            public String getIdValue(LogLevel object, int index) {
                return object.name();
            }
        });
        form.add(severityChoice);

        //TODO needed refactoring this component
        ListMultipleChoice<LogFacility> facilityChoice = new ListMultipleChoice<LogFacility>(
                "facility-choice"
                , new PropertyModel<List<LogFacility>>(filterModel, "facilities")
                , LogFacility.getLogEnumList(), new IChoiceRenderer<LogFacility>() {
            @Override
            public Object getDisplayValue(LogFacility object) {
                return object.name();
            }

            @Override
            public String getIdValue(LogFacility object, int index) {
                return object.name();
            }
        });
        form.add(facilityChoice);

        List<HostData> hostData = logCollector.loadHosts();
        ListMultipleChoice<HostData> hostDataChoice = new ListMultipleChoice<HostData>(
                "hostData-choice"
                , new PropertyModel<List<HostData>>(filterModel, "hosts")
                , hostData
                , new IChoiceRenderer<HostData>() {
            @Override
            public Object getDisplayValue(HostData object) {
                return object.getHostname();
            }

            @Override
            public String getIdValue(HostData object, int index) {
                return object.getHostname();
            }
        });
        form.add(hostDataChoice);

        form.add(new Button("search-button") {
            @Override
            public void onSubmit() {
                System.out.println(filterModel.getDateRange());
            }
        });

        IModel<List<LogData>> listDataModel = new LoadableDetachableModel<List<LogData>>() {
            @Override
            protected List<LogData> load() {
                try {
                    return logCollector.search(
                            filterModel.getDateRange().getFromDate()
                            , filterModel.getDateRange().getToDate()
                            , filterModel.getFacilityIds()
                            , filterModel.getSeverityIds()
                            , filterModel.getHostIds()
                            , filterModel.getPattern()
                            , 0
                            , 26);
                } catch (IndexerServiceException e) {
                    error("Error while retrieving log data"); //TODO fetch message from resource file
                    return Collections.emptyList();
                }
            }
        };

        ListView<LogData> listLogDataView = new ListView<LogData>("list-log-data", listDataModel) {
            @Override
            protected void populateItem(ListItem<LogData> item) {
                LogData logData = item.getModelObject();
                String logLevel = LogLevel.forValue(logData.getSeverity());
                item.add(new Label("log-date", DateFormatUtils.SMTP_DATETIME_FORMAT.format(logData.getDate().getTime())));
                item.add(new Label("log-severity", logLevel));
                item.add(new Label("log-facility", LogFacility.forValue(logData.getFacility())));
                item.add(new Label("log-host", logData.getHost()));
                item.add(new Label("log-message", logData.getMessage()));
                setHighlightCssClass(logLevel, item);
            }
        };
        add(listLogDataView);
    }

    private boolean isVisibleDateField(DateRangeType type){
        if(DateRangeType.EXACTLY_DATE.equals(type) || DateRangeType.EXACTLY_TIME.equals(type)){
            return true;
        }
        return false;
    }

    @SpringBean
    private ILogCollector logCollector;

    private DateTextField dateFromTextField;
    private DateTextField dateToTextField;
    private WebMarkupContainer holderDateRangeContainer;

    private class FilterModel implements Serializable {
        private DateRange dateRange;
        private DateRangeType dateRangeType;
        private Date exactlyDateFrom;
        private Date exactlyDateTo;

        private List<Integer> facilityIds;
        private List<LogFacility> facilities;

        private List<Integer> severityIds;
        private List<LogLevel> severities;

        private List<Integer> hostIds;
        private List<HostData> hosts;
        private String pattern;

        private FilterModel() {
            this.dateRange = DateRange.today();
            this.dateRangeType = DateRangeType.TODAY;
            this.severities = new ArrayList<LogLevel>();
            this.facilities = new ArrayList<LogFacility>();
        }

        public void setDateRangeType(DateRangeType dateRangeType) {
            this.dateRangeType = dateRangeType;
            setDateRange();
        }
        public DateRangeType getDateRangeType() {
            return dateRangeType;
        }

        public Date getExactlyDateFrom() { return exactlyDateFrom; }
        public void setExactlyDateFrom(Date exactlyDateFrom) { this.exactlyDateFrom = exactlyDateFrom; }

        public Date getExactlyDateTo() { return exactlyDateTo; }
        public void setExactlyDateTo(Date exactlyDateTo) { this.exactlyDateTo = exactlyDateTo; }

        public DateRange getDateRange() {
            if(isVisibleDateField(this.dateRangeType)){
               dateRange = new DateRange(exactlyDateFrom, exactlyDateTo);
            }
            return dateRange;
        }
        private void setDateRange() {
            switch (this.dateRangeType) {
                case TODAY:
                    dateRange = DateRange.today();
                    break;
                case YESTERDAY:
                    dateRange = DateRange.yesterday();
                    break;
                case THIS_WEEK:
                    dateRange = DateRange.thisWeek();
                    break;
                case LAST_WEEK:
                    dateRange = DateRange.lastWeek();
                    break;
                case THIS_MONTH:
                    dateRange = DateRange.thisMonth();
                    break;
                case LAST_MONTH:
                    dateRange = DateRange.lastMonth();
                    break;
                case EXACTLY_DATE:
                    break;
                case EXACTLY_TIME:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown date range type: " + this.dateRangeType);
            }
        }


        //FACILITY
        public List<Integer> getFacilityIds() { return facilityIds; }
        private void setFacilityIds(List<LogFacility> logFacilities) {
            this.facilityIds = getListIdsFromListEnum(logFacilities);
        }

        public List<LogFacility> getFacilities() { return facilities; }
        public void setFacilities(List<LogFacility> facilities) {
            this.facilities = facilities;
            setFacilityIds(facilities);
        }

        //SEVERITY
        public List<Integer> getSeverityIds() { return severityIds; }
        private void setSeverityIds(List<LogLevel> logLevels) {
            this.severityIds = getListIdsFromListEnum(logLevels);
        }

        public List<LogLevel> getSeverities() { return severities; }
        public void setSeverities(List<LogLevel> severities) {
            this.severities = severities;
            setSeverityIds(severities);
        }

        //HOST
        public List<Integer> getHostIds() { return hostIds; }
        private void setHostIds(List<HostData> hosts) {
            List<Integer> ids = new ArrayList<Integer>();
            for (HostData host : hosts) {
                ids.add(host.getId().intValue());
            }
            this.hostIds = ids;
        }

        public List<HostData> getHosts() { return hosts; }
        public void setHosts(List<HostData> hosts) {
            this.hosts = hosts;
            setHostIds(hosts);
        }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }

        private List<Integer> getListIdsFromListEnum(List<? extends LogEnum> logEnums) {
            List<Integer> ids = new ArrayList<Integer>(logEnums.size());
            for (LogEnum logEnum : logEnums) {
                ids.add(logEnum.getValue());
            }
            return ids;
        }
    }

}
