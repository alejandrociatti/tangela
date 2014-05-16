package models

import anorm.{NotAssigned, Pk}
import java.util.Calendar

/**
 * Created by Javi on 5/16/14.
 */


class InvestmentRound(id: Pk[Long] = NotAssigned, startDate: Option[Calendar], endDate: Option[Calendar]) {

}

class Investment(id: Pk[Long] = NotAssigned, investmentRound: InvestmentRound, investor: Investor) {

}

class Investor (id: Pk[Long] = NotAssigned, name: String){

}
