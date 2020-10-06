package org.matsim.run;


import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author zmeng
 */
public class RunBaseCaseHamburgScenario {

    private static final Logger log = Logger.getLogger(RunBaseCaseHamburgScenario.class);

    public static final String COORDINATE_SYSTEM = "EPSG:25832";
    public static final String VERSION = "v1.0";
    public static final int SCALE = 1;
    public static final double[] X_EXTENT = new double[]{490826.5738238178, 647310.6279172485};
    public static final double[] Y_EXTENT = new double[]{5866434.167201331, 5996884.970634732};

    public static void main(String[] args) {

        for (String arg : args) {
            log.info(arg);
        }

        if (args.length == 0) {
            args = new String[] {"../shared-svn/projects/avoev/matsim-input-files/berlin/v1/config_berlin_vsp_2019-05-08_test.xml"};
        }

        RunBaseCaseHamburgScenario baseCaseHH = new RunBaseCaseHamburgScenario();
        baseCaseHH.run(args);
    }

    private void run(String[] args) {

        Config config = prepareConfig(args);
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        controler.run();

        log.info("Done.");
    }

    public static Controler prepareControler(Scenario scenario) {
        Controler controler = new Controler(scenario);

        // use the sbb pt raptor router
        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                install( new SwissRailRaptorModule() );
            }
        } );

        // use AnalysisMainModeIdentifier instead of RoutingModeIdentifier
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(AnalysisMainModeIdentifier.class).to(DefaultAnalysisMainModeIdentifier.class);
            }
        });

        return controler;
    }

    public static Scenario prepareScenario(Config config) {

        /*
         * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
         * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
         * class is also used for runs without drt.
         */
        final Scenario scenario = ScenarioUtils.createScenario( config );

        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        ScenarioUtils.loadScenario(scenario);

        scenario.getPopulation().getPersons().values().forEach(person -> {
            if(Integer.parseInt(person.getAttributes().getAttribute("age").toString()) < 17)
                person.getAttributes().putAttribute("bannedModes",TransportMode.car);
        });

        //todo add income attribute
        boolean usePersonSpecificMarginalUtilityOfMoney = false;
        if (usePersonSpecificMarginalUtilityOfMoney) {

        }

        return scenario;
    }

    public static Config prepareConfig(String[] args, ConfigGroup... customModules) {

        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );

        // delete default modes
        config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);

        String[] typedArgs = Arrays.copyOfRange(args, 1, args.length);
        ConfigUtils.applyCommandline(config, typedArgs);

        //todo: think about opening and closing time, it can be overnight activities like shopping or business...
        for (long ii = 600; ii <= 97200; ii += 600) {

            for (String act : List.of("educ_higher", "educ_tertiary", "educ_other", "home", "educ_primary", "errands", "educ_secondary", "visit")) {
                config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(act + "_" + ii + ".0").setTypicalDuration(ii));
            }

            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("work_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("business_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("leisure_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_daily_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("shop_other_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
            config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams("educ_kiga_" + ii + ".0").setTypicalDuration(ii).setOpeningTime(8. * 3600.).setClosingTime(18. * 3600.));
        }

        return config;
    }
}