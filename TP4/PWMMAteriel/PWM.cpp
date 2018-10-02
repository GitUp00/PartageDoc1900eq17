// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)

#include <avr/interrupt.h>
#include <avr/io.h>

#define F_CPU 8000000
#include <util/delay.h>

volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0;   //-------p2


void ajustementPWM (uint8_t numerateur, uint8_t denominateur) 
{
	
	OCR1A = numerateur;
	OCR1B = denominateur;
	
	TCCR1A =  (1 << COM1A1) |(1 << COM1A0) |(1 << COM1B1) | (1 << COM1B0) |(1 << WGM10);
	TCCR1B = (0 << CS12) |(1 << CS11)| (0 << CS10);
	TCCR1C = 0;
}

int main()
{
	
    DDRA = 0xff;
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0xff; // PORT D est en mode sortie
	PIND = 0b00001000;
	ajustementPWM(0,0);
	_delay_ms(2000);
	ajustementPWM(64,64);
	_delay_ms(2000);
	ajustementPWM(128,128);
	_delay_ms(2000);
	ajustementPWM(192,192);
	_delay_ms(2000);
	ajustementPWM(255, 255);
	_delay_ms(2000);
	ajustementPWM(0,0);

    
    return 0;
}
