package models

import org.joda.time.DateTime

/**
 * Created by Javier Isoldi.
 * Date: 5/16/14.
 * Project: Tangela.
 */

class InvestmentRound(startDate: Option[DateTime], endDate: Option[DateTime])

class Investment(investmentRound: InvestmentRound, investor: Investor)

class Investor(name: String)
