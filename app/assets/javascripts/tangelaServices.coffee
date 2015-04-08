serviceModule = angular.module('app.services', [])

serviceModule.factory 'dataAccess', ['$http', ($http) ->
  declaration =
    # Location related getters
    location:
      getChildren: (locationId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Locations.getChildrenOf(locationId)).success(onSuccess).error(onError)

      getByName: (locationName, onSuccess, onError) ->
        $http(jsRoutes.controllers.Locations.getCountriesByString(locationName)).success(onSuccess).error(onError)

    # Startup related getters
    startup:
      getByLocation: (locationId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Locations.getStartupsByLocationId(locationId)).success(onSuccess).error(onError)

      getByCriteria: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.startupsCriteriaSearch(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)

      getByName: (startupName, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getStartupsByName(startupName)).success(onSuccess).error(onError)

      getWithTags: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.startupCriteriaSearchAndTags(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)

      getFounderAmount: (startupId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getNumberOfFoundersByStartupId(startupId)).success(onSuccess).error(onError)

      getFunding: (startupId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getStartupFunding(startupId)).success(onSuccess).error(onError)

      getFundingsByCriteria: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.startupsFundingByCriteria(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)

      getNetwork: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Networks.getStartupsNetwork(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)  

    # User/People related getters
    user:
      getByStartupCriteria: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getUsersInfoByCriteria(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          )).success(onSuccess).error(onError)

      getNetwork: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Networks.getPeopleNetwork(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)

      getNetwork2: (criteriaObj, onSuccess, onError) ->
        $http(jsRoutes.controllers.Networks.getPeopleNetwork2ndOrder(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        )).success(onSuccess).error(onError)

  # CSV URL getters
    csv:
      url:
        startupsNetwork: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getStartupsNetworkCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        peopleNetwork: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getPeopleNetworkCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        peopleNetwork2: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getPeopleNetwork2ndOrderCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        startups: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getStartupsCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        users: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getUsersCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        tags: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getStartupsTagsCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        roles: (startupId) -> jsRoutes.controllers.CSVs.getStartupRolesCSV(startupId).url

        funding: (startupId) -> jsRoutes.controllers.CSVs.getStartupFundingCSV(startupId).url

        fundings: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getStartupsFundingsCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

    # Getters for graph making
    graph:
      getStartup: (startupId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getStartupNetInfo(startupId)).success(onSuccess).error(onError)

      getStartupRoles: (startupId, onSuccess, onError) ->
        $http(jsRoutes.controllers.Startups.getRolesOfStartup(startupId)).success(onSuccess).error(onError)

  declaration # We return the created object, which is indeed the so called 'service'
]