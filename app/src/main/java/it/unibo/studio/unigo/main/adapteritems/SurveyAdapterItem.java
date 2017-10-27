package it.unibo.studio.unigo.main.adapteritems;

import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.unibo.studio.unigo.utils.firebase.Survey;
import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;

public class SurveyAdapterItem
{
    private List<View> legend;
    private Survey survey;
    private String survey_key;
    private boolean isInitialized;
    private HashMap<String, SliceValue> sliceMap;
    private PieChartData chartdata;

    public SurveyAdapterItem(String questionKey, Survey survey)
    {
        this.survey = survey;
        this.survey_key = questionKey;
        this.isInitialized = false;
        this.legend = new ArrayList<>();
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public String getSurveyKey()
    {
        return survey_key;
    }

    public boolean isInitialized()
    {
        return isInitialized;
    }

    public void setInitialized()
    {
        isInitialized = true;
    }

    public void setGraphLegend(View legend)
    {
        this.legend.add(legend);
    }

    public List<View> getGraphLegend()
    {
        if (legend != null)
            return legend;
        return null;
    }

    public void setGraphSlice(HashMap<String, SliceValue> sliceList)
    {
        this.sliceMap = sliceList;
    }

    public HashMap<String, SliceValue> getGraphSlice()
    {
        if (sliceMap != null)
            return sliceMap;
        return null;
    }

    public void setPieChartData(PieChartData chartData)
    {
        this.chartdata = chartData;
    }

    public PieChartData getPieChartData()
    {
        if (chartdata != null)
            return  chartdata;
        return null;
    }
}