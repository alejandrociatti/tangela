serviceModule = angular.module('app.services', [])

serviceModule.factory 'dataAccess', ['$http', ($http) ->
  declaration =
    # Location related getters
    location:
      getChildren: (locationId) -> $http(jsRoutes.controllers.Locations.getChildrenOf(locationId))

      getByName: (locationName) -> $http(jsRoutes.controllers.Locations.getCountriesByString(locationName))

    # Startup related getters
    startup:
      getByLocation: (locationId) -> $http(jsRoutes.controllers.Locations.getStartupsByLocationId(locationId))

      getByCriteria: (criteriaObj) ->
        $http(jsRoutes.controllers.Startups.startupsCriteriaSearch(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

      getByName: (startupName) -> $http(jsRoutes.controllers.Startups.getStartupsByName(startupName))

      getWithTags: (criteriaObj) ->
        $http(jsRoutes.controllers.Startups.startupCriteriaSearchAndTags(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

      getFounderAmount: (startupId) -> $http(jsRoutes.controllers.Startups.getNumberOfFoundersByStartupId(startupId))

      getFunding: (startupId) -> $http(jsRoutes.controllers.Startups.getStartupFunding(startupId))

      getFundingsByCriteria: (criteriaObj) ->
        $http(jsRoutes.controllers.Startups.startupsFundingByCriteria(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

      getNetwork: (criteriaObj) ->
        $http(jsRoutes.controllers.Networks.getStartupsNetwork(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

    # User/People related getters
    user:
      getByStartupCriteria: (criteriaObj) ->
        $http(jsRoutes.controllers.Startups.getUsersInfoByCriteria(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ))

      getNetwork: (criteriaObj) ->
        $http(jsRoutes.controllers.Networks.getPeopleNetwork(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

      getNetwork2: (criteriaObj) ->
        $http(jsRoutes.controllers.Networks.getPeopleNetwork2ndOrder(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

      getWithRoles: (criteriaObj) ->
        $http(jsRoutes.controllers.Startups.getUserAndRolesByCriteria(
          criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
        ))

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

        usersAndRoles: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getUsersAndRolesCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        startupsTags: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getStartupsTagsCSV(
            criteriaObj.location, criteriaObj.market, criteriaObj.quality, criteriaObj.date
          ).url

        usersTags: (criteriaObj) ->
          jsRoutes.controllers.CSVs.getPeopleTagsCSV(
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
      getStartup: (startupId) ->
        $http(jsRoutes.controllers.Startups.getStartupNetInfo(startupId))

      getStartupRoles: (startupId) ->
        $http(jsRoutes.controllers.Startups.getRolesOfStartup(startupId))

  declaration # We return the created object, which is indeed the so called 'service'
]