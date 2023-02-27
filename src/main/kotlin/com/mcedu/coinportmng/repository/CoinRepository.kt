package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.Coin
import org.springframework.data.jpa.repository.JpaRepository

interface CoinRepository: JpaRepository<Coin, String>