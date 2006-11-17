/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Jul 2, 2005
 * Time: 12:16:02 AM
 */
package com.theoryinpractice.testng;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.intellij.execution.ExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.RunJavaConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.runners.RunnerInfo;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.theoryinpractice.testng.model.TestData;
import com.theoryinpractice.testng.model.TestType;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class TestNGConfiguration extends CoverageEnabledConfiguration implements RunJavaConfiguration
{
    //private TestNGResultsContainer resultsContainer;
    protected TestData data;
    protected transient Project project;
    public boolean ALTERNATIVE_JRE_PATH_ENABLED;
    public String ALTERNATIVE_JRE_PATH;

    public TestNGConfiguration(String s, Project project, ConfigurationFactory factory) {
        this(s, project, new TestData(), factory);
    }

    private TestNGConfiguration(String s, Project project, TestData data, ConfigurationFactory factory) {
        super(s, new RunConfigurationModule(project, false), factory);
        this.data = data;
        this.project = project;
    }

    public RunProfileState getState(DataContext dataContext, RunnerInfo runnerInfo, RunnerSettings runnerSettings, ConfigurationPerRunnerSettings configurationPerRunnerSettings) {
        return new TestNGRunnableState(
                runnerSettings,
                configurationPerRunnerSettings,
                this);
    }

    public TestData getPersistantData() {
        return data;
    }

    @NotNull
    public String getCoverageFileName() {
        return "testng-coverage.dat";
    }

    protected boolean isMergeDataByDefault() {
        return false;
    }

    @Override
    protected ModuleBasedConfiguration createInstance() {
        try {
            return new TestNGConfiguration(getName(), getProject(), (TestData) data.clone(), TestNGConfigurationType.getInstance().getConfigurationFactories()[0]);
        }
        catch (CloneNotSupportedException e) {
            //can't happen right?
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection getValidModules() {
        //TODO add handling for package
        return RunConfigurationModule.getModulesForClass(getProject(), data.getMainClassName());
    }

    @Override
    public boolean isGeneratedName() {
        return data.isGeneratedName(getName(), getConfigurationModule());
    }

    @Override
    public String suggestedName() {
        if (TestType.CLASS.getType().equals(data.TEST_OBJECT)) {
            String shortName = ExecutionUtil.getShortClassName(data.MAIN_CLASS_NAME);
            return ExecutionUtil.shortenName(shortName, 0);
        }
        if (TestType.PACKAGE.getType().equals(data.TEST_OBJECT)) {
            String s = getName();
            if (!isGeneratedName())
                return '\"' + s + '\"';
            if (data.getPackageName().trim().length() > 0)
                return "Tests in \"" + data.getPackageName() + '\"';
            else
                return "All Tests";
        }
        if (TestType.METHOD.getType().equals(data.TEST_OBJECT)) {
            return data.getMethodName() + "()";
        }
        return data.getGroupName();
    }

    public void setProperty(int type, String value) {
        data.setProperty(type, value, project);
    }

    public String getProperty(int type) {
        return data.getProperty(type, project);
    }

    public void setClassConfiguration(PsiClass psiclass) {
        setModule(data.setMainClass(psiclass));
        data.TEST_OBJECT = TestType.CLASS.getType();
        setGeneratedName();
    }

    public void setMethodConfiguration(Location<PsiMethod> location) {
        setModule(data.setTestMethod(location));
        setGeneratedName();
    }

    public void setGeneratedName() {
        setName(getGeneratedName());
    }

    public String getGeneratedName() {
        return data.getGeneratedName(getConfigurationModule());
    }

    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        //return new TestNGConfigurationEditor(getProject());
        return new TestNGMainConfigurationEditor(getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        //TODO add various checks here
    }

    @Override
    public void readExternal(Element element)
            throws InvalidDataException {
        super.readExternal(element);
        readModule(element);
        DefaultJDOMExternalizer.readExternal(this, element);
        DefaultJDOMExternalizer.readExternal(getPersistantData(), element);

        Map<String, String> properties = getPersistantData().TEST_PROPERTIES;
        Element propertiesElement = element.getChild("properties");
        if (propertiesElement != null) {
            List<Element> children = propertiesElement.getChildren("property");
            for (Element property : children) {
                properties.put(property.getAttributeValue("name"), property.getAttributeValue("value"));
            }
        }
    }

    @Override
    public void writeExternal(Element element)
            throws WriteExternalException {
        super.writeExternal(element);
        writeModule(element);
        DefaultJDOMExternalizer.writeExternal(this, element);
        DefaultJDOMExternalizer.writeExternal(getPersistantData(), element);

        Element propertiesElement = element.getChild("properties");
        if (propertiesElement == null) {
            propertiesElement = new Element("properties");
            element.addContent(propertiesElement);
        }

        Map<String, String> properties = getPersistantData().TEST_PROPERTIES;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Element property = new Element("property");
            property.setAttribute("name", entry.getKey());
            property.setAttribute("value", entry.getValue());
            propertiesElement.addContent(property);
        }

    }

}